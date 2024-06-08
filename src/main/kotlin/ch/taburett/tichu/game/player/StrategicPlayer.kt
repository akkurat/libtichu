package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.game.protocol.GiftDragon.ReLi.LI
import ch.taburett.tichu.game.protocol.GiftDragon.ReLi.RE
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import ch.taburett.tichu.patterns.TichuPatternType.SINGLE


class StrategicPlayer(val listener: (PlayerMessage) -> Unit) : Round.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: Player) {
        val response = strategic(message)
        if (response != null) listener(response)
    }

    override val type: String = "Strat"

    override fun toString(): String {
        return type
    }


    fun strategic(message: ServerMessage): PlayerMessage? {
        return when (message) {
            is AckGameStage -> ack(message)
            is Schupf -> Ack.SchupfcardReceived()
            is WhosMove -> move(message)
            else -> null
        }
    }

    private fun move(
        wh: WhosMove,
    ): PlayerMessage? {
        return if (wh.stage == Stage.GIFT_DRAGON) {
            val nRe = wh.cardCounts.getValue(wh.youAre.re)
            val nLi = wh.cardCounts.getValue(wh.youAre.li)
            if (nRe > nLi) GiftDragon(LI) else GiftDragon(RE)
        } else if (wh.stage == Stage.YOURTURN) {

            val move =
                if (wh.table.isEmpty()) {
                    openingMove(wh)
                } else {
                    reactionMove(wh)
                }

            move
        } else {
            null
        }
    }

    private fun openingMove(wh: WhosMove): Move {
        val handcards = wh.handcards
        val mightFullfillWish = mightFullfillWish(handcards, wh.wish)
        if (handcards.isEmpty()) {
            return move(listOf())
        }


        var (pats, orphans) = _allPatterns(handcards)



        if (mightFullfillWish) {
            pats = pats.filter { it.cards.any { it.getValue() - wh.wish!! == 0.0 } }.toSet()
            orphans = orphans.filter { it.card.getValue() - wh.wish!! == 0.0 }.toSet()
        }


        val result = emulateRandom(handcards, pats + orphans, wh.goneCards, wh.cardCounts, wh.youAre)

//        val cards = if (orphans.any { it.rank() < ORPH } || result.isEmpty()) {
//            setOf(orphans.minBy { it.rank() }.card)
//        } else {
////            (pats + orphans).filter { it !is Single || it.card.getSort() >= ORPH }
////                .minBy { it.rank() }.cards
//            result.maxBy { it.value }.key.cards
//        }

        val cards = result.maxBy { it.value }.key.cards

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

    private fun _allPatterns(handcards: List<HandCard>): Pair<Set<TichuPattern>, Set<Single>> {
        val pats = allPatterns(handcards).filter { it.type != SINGLE }.toSet()

        val orphans = Single.allPatterns(handcards - pats.flatMap { it.cards })
        return Pair(pats, orphans)
    }

    private fun emulateRandom(
        handcards: List<HandCard>,
        pats: Set<TichuPattern>,
        _goneCards: Set<PlayCard>,
        cardCounts: Map<Player, Int>,
        iam: Player,
    ): Map<TichuPattern, Double> {

        val restcards = fulldeck - _goneCards - handcards


        val nPartner = cardCounts.getValue(iam.partner)
        val nRe = cardCounts.getValue(iam.re)
        val nLi = cardCounts.getValue(iam.li)
        val out = pats.associateWith { mutableListOf<Int>() }
        for (i in 1..20) {

            val (partner, left, right) = randomCards(restcards, nPartner, nRe, nLi)

            val cardMap = mapOf(iam to handcards, iam.partner to partner, iam.re to right, iam.li to left)

            // todo: make available generic function to simulate with other players
            for (pat in pats) {
                // until all pass
                val tricks = Tricks()
                val mutableDeck = MutableDeck(cardMap, iam)
                var youAre = iam
                var cards: Collection<PlayCard> = pat.cards
                do {
                    mutableDeck.playCards(youAre, cards)
                    tricks.add(PlayLogEntry(youAre, cards))
                    if (mutableDeck.cards(youAre).isEmpty()) {
                        tricks.add(PlayerFinished(youAre))
                    }
                    if (cards.contains(DOG)) {
                        tricks.endTrick()
                    }

                    if (tricks.table.allPass(mutableDeck.activePlayers())) {
                        tricks.endTrick()
                    }
                    if (mutableDeck.roundEnded()) {
                        tricks.endTrick()
                        break
                    }
                    youAre = tricks.nextPlayer(mutableDeck)
                    val move = stupidMove(
                        WhosMove(
                            youAre, youAre, mutableDeck.cards(youAre), tricks.table,
                            null, null, false, mutableDeck.deckSizes(), mutableDeck.goneCards()
                        )
                    )
                    if (move is Move) {
                        cards = move.cards
                    } else {
                        cards = listOf()
                    }

                } while (true)
                // now what? count moves

                val ow = tricks.orderWinning

                val points = when (ow.indexOf(iam)) {
                    -1 -> -20
                    0 -> 20
                    1 -> 5
                    else -> 3
                }


                // partner makes only sense when schupfed cards are taken into account
//                if(ow.indexOf(iam) == 0) {
//                    points += 10
//                    if(ow.indexOf(iam.partner)==1) {
//                        points += 30
//                    }
//                }
//                if(ow.indexOf(iam.partner) == 0) points += 10
//                if(ow.indexOf(iam.re) == 0 ) points -= 10
//                if(ow.indexOf(iam.li) == 0 ) points -= 10


                out[pat]?.add(points)
            }
//            println(out)
        }
        val av = out.mapValues { it.value.average() }

        return av

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
    ): PlayerMessage? {
        return when (message.stage) {
            Stage.EIGHT_CARDS -> Ack.BigTichu()
            Stage.PRE_SCHUPF -> Ack.TichuBeforeSchupf()
            Stage.SCHUPF -> {
                val cards = message.cards
                Schupf(cards[0], cards[1], cards[2])
            }

            Stage.POST_SCHUPF -> Ack.TichuBeforePlay()
            else -> null
        }
    }
}

interface TestInter {

}

