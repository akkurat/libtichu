package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.game.protocol.GiftDragon.ReLi.LI
import ch.taburett.tichu.game.protocol.GiftDragon.ReLi.RE
import ch.taburett.tichu.patterns.Empty
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import ch.taburett.tichu.patterns.TichuPatternType.SINGLE
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.math.abs


class StrategicPlayer(val listener: (PlayerMessage) -> Unit) : BattleRound.AutoPlayer {
    @OptIn(DelicateCoroutinesApi::class)
    override fun receiveMessage(message: ServerMessage, player: Player) {
//        GlobalScope.launch {
        val response = strategic(message)
        if (response != null) listener(response)
//        }
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


        var result = emulateRandom(handcards, pats + orphans, wh.goneCards,
            wh.cardCounts, wh.youAre, wh.tricks)

        mapPatternValues(result.keys, handcards)

        result = result.mapValues { (k, v) ->
            if (k is Single) v + cardGettingRidIncentive(k.card.asHandcard()) else v
        }

        val numPatsPerCard = pats.flatMap { it.cards }.groupingBy { it.asHandcard() }.eachCount()


        val penalties = result.mapValues { (k, v) ->
            k.cards.map { numPatsPerCard[it.asHandcard()] }.sumOf { (it ?: 0) * (-10) }
        }


        val cards = result.maxBy { it.value }.key.cards

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
        goneCards: Set<PlayCard>,
        cardCounts: Map<Player, Int>,
        iam: Player,
        imTable: ImmutableTricks,
    ): Map<TichuPattern, Double> {

        val restcards = fulldeck - goneCards.map { it.asHandcard() } - handcards

        val nPartner = cardCounts.getValue(iam.partner)
        val nRe = cardCounts.getValue(iam.re)
        val nLi = cardCounts.getValue(iam.li)
        val out = pats.associateWith { mutableListOf<Int>() }
        for (i in 1..10) {

            val (partner, left, right) = randomCards(restcards, nPartner, nRe, nLi)

            for (pat in pats) {
                val cardMap = mapOf(
                    iam to handcards - pat.cards.map { it.asHandcard() },
                    iam.partner to partner,
                    iam.re to right,
                    iam.li to left
                )

                val deck = MutableDeck.createStarted(cardMap, iam, goneCards + pat.cards)
                val tricks = MutableTricks(imTable)
                // logic for adding a trick must take more responsiblity
                tricks.add(RegularMoveEntry(iam, pat.cards))
                if(pat.cards.contains(DOG)) {
                    tricks.endTrick()
                }


                val sim = SimulationRound(deck, tricks) { p, com ->
//                    if (p == iam && handcards.size==14 ) StrategicPlayer(com) else StupidPlayer(com)
                    StupidPlayer(com)
                }
                val result = sim.start()
                if (result.finished) {
                    val ri = result.roundPlay
                    val ow = ri.tricks.orderWinning


                    var points: Int = 0
                    try {
                        val totalPoints = ri.getRoundInfo().totalPoints
                        points += totalPoints.getValue(iam.playerGroup)
                        points -= totalPoints.getValue(iam.playerGroup.other())
                    } catch (_: Exception) {
                        //todo: // more generic counting
                        // however, not possible correctly without all tricks of the game
                    } /// uuuugly

                    points += when (ow.indexOf(iam)) {
                        -1 -> -30
                        0 -> 120
                        1 -> 40
                        else -> 10
                    }



                    out[pat]?.add(points)
                }
            }
//            println(out)
        }
        val av = out.mapValues { it.value.average() }
        val mx = av.values.maxOf { abs(it) }


        return av.mapValues { it.value / mx }
    }


    private fun reactionMove(wh: WhosMove): Move {
        val table = wh.table
        val handcards = wh.handcards
        val pat = pattern(table.toBeatCards())

        var beatingPatterns = pat.findBeatingPatterns(handcards).toMutableSet()

        if (wh.wish != null) {
            val allWithWish = beatingPatterns
                .filter { p -> p.cards.any { c -> (c.getValue() - wh.wish) == 0.0 } }
                .toMutableSet()
            if (allWithWish.isNotEmpty()) {
                beatingPatterns = allWithWish
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

        val pats = _allPatterns(handcards)

        val simlated =
            emulateRandom(handcards, beatingPatterns + Empty(), wh.goneCards, wh.cardCounts, wh.youAre, wh.tricks)

        val bestPat = simlated.maxBy { it.value }

        return Move(bestPat.key.cards)


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

