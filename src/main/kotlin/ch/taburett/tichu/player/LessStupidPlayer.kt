package ch.taburett.tichu.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.communication.Message.*
import ch.taburett.tichu.game.communication.createMove
import ch.taburett.tichu.game.communication.moveSingle
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import ch.taburett.tichu.patterns.TichuPatternType.SINGLE


private class LessStupidPlayer(val listener: PlayerMessageConsumer) : BattleRound.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: EPlayer) {
        lessStupidMove(message, player)
    }

    override fun toString() = type

    override val type: String = "LessStupid"

    private fun lessStupidMove(message: ServerMessage, player: EPlayer) {
        when (message) {
            is AckGameStage -> ack(message)
            is Schupf -> listener.accept(Ack.SchupfcardReceived())
            is WhosMove -> move(message)
            else -> {}
        }
    }

    private fun move(wh: WhosMove) {
        if (wh.stage == Stage.GIFT_DRAGON) {
            listener.accept(GiftDragon(GiftDragon.ReLi.LI))
        } else if (wh.stage == Stage.YOURTURN) {

            val move =
                if (wh.table.isEmpty()) {
                    openingMove(wh)
                } else {
                    reactionMove(wh)
                }

            listener.accept(move)
        }
    }

    private fun probabilitiesByMessage(wh: WhosMove): Map<TichuPattern, Double> {
        val myPatterns = allPatterns(wh.handcards).toSet()
        return prob(
            wh.handcards, wh.goneCards,
            myPatterns, wh.cardCounts.getValue(wh.youAre.partner),
            wh.cardCounts.getValue(wh.youAre.li),
            wh.cardCounts.getValue(wh.youAre.re)
        )
    }

    private fun reactionMove(wh: WhosMove): Move {
        val table = wh.table
        val handcards = wh.handcards
        val pat = pattern(table.toBeatCards())

        var beatingPatterns = pat.findBeatingPatterns(handcards).toMutableSet()

        if (pat is Single) {
            if (handcards.contains(DRG)) {
                beatingPatterns.add(Single(DRG))
            }
            if (pat.card != DRG && handcards.contains(PHX)) {
                beatingPatterns.add(Single(PHX.asPlayCard(pat.card.getValue() + .5)))
            }
        }

        if (wh.wish != null) {
            val allWithWish = beatingPatterns
                .filter { p -> p.cards.any { c -> (c.getValue() - wh.wish) == 0.0 } }
                .toMutableSet()
            if (allWithWish.isNotEmpty()) {
                beatingPatterns = allWithWish
            }
        }


        // todo: if cheapestbreating cheaper than cheapest at all?
        // take into account cards of other players

//    val prices = beatingPatterns.associateWith { weightPossibilites(it, handcards, 0, 1.0) }
        val prices = weightPossibilitesNoRec(handcards)
        val beatingPrices = mapPatternValues(beatingPatterns, handcards)


        return if (beatingPrices.isNotEmpty()) {

            val beatingCheapest = beatingPrices.minBy { it.value }

            val lower = prices.filter { it.value < beatingCheapest.value }
            // not sure if pattern or card in those patterns should be counted
            val ratio = lower.size.toDouble() / prices.size < 0.3

            if (ratio && (table.toBeat()?.player?.playerGroup != wh.youAre.playerGroup || pat.rank() < 10)
                || wh.wish != null
            ) {
                createMove(beatingCheapest.key.cards)
            } else {
                createMove(listOf())
            }
        } else {
            createMove(listOf())
        }
    }

    private val ORPHAN_VALUE = 7

    private fun openingMove(
        wh: WhosMove,
    ): Move {
        val handcards = wh.handcards
        val mightFullfillWish = mightFullfillWish(handcards, wh.wish)
        val move =
            if (mightFullfillWish) {
                // todo: might try to play normal pattern as well
                val numberCard = handcards
                    .filterIsInstance<NumberCard>()
                    .filter { nc -> wh.wish!! - nc.getValue() == 0.0 }
                    .minByOrNull { it.getValue() }
                moveSingle(numberCard)
            } else {

//            val prob = probabilitiesByMessage(wh)
                val price = weightPossibilitesNoRec(handcards)

                val cards = if (price.isNotEmpty()) {

                    val orphans = handcards - price.keys.filter { it.type != SINGLE }.flatMap { it.cards }.toSet()

                    if (handcards.size == 1 && handcards[0] == PHX) {
                        setOf(PHX.asPlayCard(1.5))
                    } else if (orphans.any { it.getSort() < ORPHAN_VALUE }) {
                        setOf(orphans.filterIsInstance<PlayCard>().minBy { it.getSort() })
                    } else {
                        price.filter { it.key !is Single || (it.key as Single).card.getSort() >= ORPHAN_VALUE }
                            .minBy { it.value }.key.cards
                    }
                } else {
                    val c = handcards.minBy { it.getSort() }
                    when (c) {
                        is Phoenix -> setOf(PHX.asPlayCard(1.5))
                        is PlayCard -> setOf(c)
                        else -> setOf()
                    }
                }

                if (cards.contains(MAH)) {
                    val ownValues = handcards.filterIsInstance<NumberCard>()
                        .map { it.getValue().toInt() }
                        .toSet()
                    val rw = (2..14) - ownValues
                    val randomWish: Int = rw.shuffled().first()
                    createMove(cards, randomWish)
                } else {
                    createMove(cards)
                }
            }
        return move
    }


    private fun ack(message: AckGameStage) {
        when (message.stage) {
            Stage.EIGHT_CARDS -> listener.accept(Ack.BigTichu())
            Stage.PRE_SCHUPF -> listener.accept(Ack.TichuBeforeSchupf())
            Stage.SCHUPF -> {
                val cards = message.handcards
                listener.accept(Schupf(cards[0], cards[1], cards[2]))
            }

            Stage.POST_SCHUPF -> listener.accept(Ack.TichuBeforePlay())
            else -> {}
        }
    }
}
