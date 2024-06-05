package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPatternType.SINGLE
import java.util.*
import java.util.function.Consumer


class StrategicPlayer(val listener: (PlayerMessage) -> Unit) : Round.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: Player) {
        strategic(message, listener, player)
    }

    override val type: String = "Strat"

    override fun toString(): String {
        return type
    }


    fun strategic(message: ServerMessage, listener: Consumer<PlayerMessage>, player: Player) {
        when (message) {
            is AckGameStage -> ack(message, listener)
            is Schupf -> listener.accept(Ack.SchupfcardReceived())
            is WhosMove -> move(message, listener)
            else -> {}
        }
    }

    private fun move(
        wh: WhosMove,
        listener: Consumer<PlayerMessage>,
    ) {
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

    private fun openingMove(wh: WhosMove): Move {
        val handcards = wh.handcards
        val mightFullfillWish = mightFullfillWish(handcards, wh.wish)
        if (handcards.isEmpty()) {
            return move(listOf())
        }


        var pats = allPatterns(handcards).filter { it.type != SINGLE }.toSet()


        var orphans = Single.allPatterns(handcards - pats.flatMap { it.cards })


        val prob = wh.probabilitiesByMessage(pats)
        val oprop = wh.probabilitiesByMessage(orphans)


        if (mightFullfillWish) {
            pats = pats.filter { it.cards.any { it.getValue() - wh.wish!! == 0.0 } }.toSet()
            orphans = orphans.filter { it.card.getValue() - wh.wish!! == 0.0 }.toSet()
        }

        val cards = if (orphans.any { it.rank() < ORPH }) {
            setOf(orphans.minBy { it.rank() }.card)
        } else {
            (pats+orphans).filter { it !is Single || it.card.getSort() >= ORPH }
                .minBy { it.rank() }.cards
        }

//        {
//            val c = handcards.minBy { it.getSort() }
//            when (c) {
//                is Phoenix -> setOf(PHX.asPlayCard(1.5))
//                is PlayCard -> setOf(c)
//                else -> setOf()
//            }
//        }

        val move = if (cards.contains(MAH)) {
            val ownValues = handcards.filterIsInstance<NumberCard>()
                .map { it.getValue().toInt() }
                .toSet()
            val rw = (2..14) - ownValues
            val randomWish: Int? = rw.randomOrNull()
            move(cards, randomWish)
        } else {
            move(cards)
        }
        return move
    }



    private fun reactionMove(wh: WhosMove): Move {
        val table = wh.table
        val handcards = wh.handcards
        val toBeat = table.toBeat()
        val pat = pattern(toBeat.cards)

        var beatingPatterns = pat.findBeatingPatterns(handcards).toMutableSet()

        if (wh.wish != null) {
            val allWithWish = beatingPatterns
                .filter { p -> p.cards.any { c -> (c.getValue() - wh.wish) == 0.0 } }
                .toMutableSet()
            if (allWithWish.isNotEmpty()) {
                beatingPatterns = allWithWish
                return move(allWithWish.first().cards)
            }
        }

        if (pat is Single) {
            if (handcards.contains(DRG)) {
                beatingPatterns.add(Single(DRG))
            }
            if (pat.card != DRG && handcards.contains(PHX)) {
                beatingPatterns.add(Single(PHX.asPlayCard(pat.card.getValue() + .5)))
            }
        }
        // todo: if cheapestbreating cheaper than cheapest at all?
        // take into account cards of other players

//    val prices = beatingPatterns.associateWith { weightPossibilites(it, handcards, 0, 1.0) }
        val prices = weightPossibilitesNoRec(handcards)
        val beatingPrices = prices.filterKeys { beatingPatterns.contains(it) }


        return if (beatingPrices.isNotEmpty()) {

            val beatingCheapest = beatingPrices.minBy { it.value }

            val lower = prices.filter { it.value < beatingCheapest.value }
            // not sure if pattern or card in those patterns should be counted
            val ratio = lower.size.toDouble() / prices.size < 0.3

            if (ratio && (toBeat.player.playerGroup != wh.youAre.playerGroup || pat.rank() < 10)) {
                move(beatingCheapest.key.cards)
            } else {
                move(listOf())
            }
        } else {
            move(listOf())
        }
    }

    private val ORPH = 7

    private fun ack(
        message: AckGameStage,
        listener: Consumer<PlayerMessage>,
    ) {
        when (message.stage) {
            Stage.EIGHT_CARDS -> listener.accept(Ack.BigTichu())
            Stage.PRE_SCHUPF -> listener.accept(Ack.TichuBeforeSchupf())
            Stage.SCHUPF -> {
                val cards = message.cards
                listener.accept(Schupf(cards[0], cards[1], cards[2]))
            }

            Stage.POST_SCHUPF -> listener.accept(Ack.TichuBeforePlay())
            else -> {}
        }
    }
}

