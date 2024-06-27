package ch.taburett.tichu.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.gamelog.IPlayLogEntry
import ch.taburett.tichu.game.protocol.CardsMessage
import ch.taburett.tichu.game.protocol.Message.*
import ch.taburett.tichu.game.protocol.Message.GiftDragon.ReLi.LI
import ch.taburett.tichu.game.protocol.Message.GiftDragon.ReLi.RE
import ch.taburett.tichu.game.protocol.createMove
import ch.taburett.tichu.patterns.*
import ch.taburett.tichu.patterns.TichuPatternType.SINGLE
import java.util.function.Consumer
import kotlin.Pair
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt


class StrategicPlayer(val listener: (PlayerMessage) -> Unit) : BattleRound.AutoPlayer {
    constructor(listener: Consumer<PlayerMessage>) : this({ listener.accept(it) })



    override fun receiveMessage(message: ServerMessage, player: Player) {
        if (message is WhosMove && message.handcards.size == 14) {
            if (evaluateSmallTichuAfterSchupf(message)) {
                listener(Announce.SmallTichu())
            }
        }
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
            return createMove(listOf())
        }



        var (pats, orphans) = _allPatterns(handcards, true)


        if (mightFullfillWish) {
            pats = pats.filter { it.cards.any { it.getValue() - wh.wish!! == 0.0 } }.toSet()
            orphans = orphans.filter { it.card.getValue() - wh.wish!! == 0.0 }.toSet()
        }


        var result = emulateRandom(
            handcards, pats + orphans, wh.goneCards,
            wh.cardCounts, wh.youAre, wh.tricks
        )

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
            createMove(cards, randomWish)
        } else {
            createMove(cards)
        }
        return move
    }

    private fun _allPatterns(handcards: List<HandCard>, incPhx: Boolean): Pair<Set<TichuPattern>, Set<Single>> {
        val pats = allPatterns(handcards, incPhx).filter { it.type != SINGLE }.toSet()

        val orphans = Single.allPatterns(handcards - pats.flatMap { it.cards }, incPhx = incPhx)
        return Pair(pats, orphans)
    }

    private fun emulateRandom(
        handcards: List<HandCard>,
        pats: Set<TichuPattern>,
        goneCards: Set<PlayCard>,
        cardCounts: Map<Player, Int>,
        iam: Player,
        imTable: Tricks,
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
                val move =
                    if (pat.cards.isEmpty()) IPlayLogEntry.PassMoveEntry(iam) else IPlayLogEntry.RegularMoveEntry(
                        iam,
                        pat.cards
                    )
                tricks.add(move)
                if (pat.cards.contains(DOG)) {
                    tricks.endTrick()
                }

                val sim = SimulationRound(deck, tricks) { p, com ->
//                    if (p == iam && handcards.size==14 ) StrategicPlayer(com) else StupidPlayer(com)
                    StupidPlayer(com)
                }
                try {

                    val result = sim.start()
                    if (result is SimpleBattle.BattleResult) {
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
                } catch (bl: Exception) {
                    println("who cares $bl")
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

        val pats = _allPatterns(handcards, true)

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
            Stage.EIGHT_CARDS -> if (evalBigTichu(message)
            ) {
                Announce.BigTichu()
            } else {
                Ack.BigTichu()
            }

            Stage.PRE_SCHUPF -> if (evaluateSmallTichuBeforeSchupf(message)) Announce.SmallTichu() else Ack.TichuBeforeSchupf()
            Stage.SCHUPF -> schupf(message)
            Stage.POST_SCHUPF -> if (evaluateSmallTichuAfterSchupf(message)) Announce.SmallTichu() else Ack.TichuBeforePlay()
            else -> null
        }
    }


    private fun schupf(message: AckGameStage): Schupf {
        val anytichu = message.tichuMap.any { it.value != ETichu.NONE }
        val availableCards = message.handcards.sorted().toMutableList()

        val partnerTichu = message.tichuMap.getValue(message.youAre.partner)
        if (partnerTichu != ETichu.NONE) {
            availableCards.remove(DOG) // keep dog if available
        }


        val one = removeLeastViable(availableCards)
        val two = removeLeastViable(availableCards)

        var le: PlayCard
        var re: PlayCard

        if (one.getValue() == two.getValue()) {
            re = one
            le = two
        } else {
            if (one.getValue() % 2 == 0.0) {
                re = one
                if (two.getValue() % 2 == 1.0) {
                    le = two
                } else {
                    le = removeLeastViable(availableCards)
                }
            } else {
                le = one
                if (two.getValue() % 2 == 0.0) {
                    re = two
                } else {
                    re = removeLeastViable(availableCards)
                }
            }
        }

        val tichu = message.tichuMap.getValue(message.youAre)
        val partner = if (tichu != ETichu.NONE) {
            if (availableCards.remove(DOG)) {
                DOG
            } else {
                removeLeastViable(availableCards)
            }
        } else {
            availableCards.removeLast()
        }
        return Schupf(re, le, partner)
    }

    // todo: do not remove, just return
    private fun removeLeastViable(cards: MutableList<HandCard>): PlayCard {
        val pats = allPatterns(cards, false)
        val ratings = cards.filterIsInstance<PlayCard>().associateWith { 0 }.toMutableMap()
        for (pat in pats) {

            when (pat) {
                is Straight -> {
                    val grp = cards.filterIsInstance<PlayCard>().groupBy { it.getValue() }
                    for ((index, c) in pat.cards.sortedDescending().withIndex()) {
                        val rating = if (index >= 5) 10 else 50
                        if (grp.getValue(c.getValue()).size == 1) {
                            ratings[c] = ratings[c]!! + rating
                        }
                    }
                }

                is Triple -> { // poss. bomb
                    for (c in pat.cards) {
                        ratings[c] = ratings[c]!! + 100
                    }
                }

                else -> {
                    for (c in pat.cards) {
                        ratings[c] = (ratings[c]!! + cardCost(c.asHandcard()) * 15).roundToInt()
                    }
                }

            }
        }
        val card = ratings.minBy { it.value }.key
        cards.remove(card)
        return card
    }

    private fun evalBigTichu(message: AckGameStage): Boolean {

        val cards = message.handcards
        val iam = message.youAre

        if (message.tichuMap.getValue(iam) != ETichu.NONE ||
            message.tichuMap.getValue(iam.partner) != ETichu.NONE ||
            cards.size != 8
        ) {
            return false
        }
        val (heightness, orphPenalties) = evaluateCardsBeforeSchupf(cards)
        return heightness - orphPenalties > 17
    }

    private fun evaluateSmallTichuBeforeSchupf(message: AckGameStage): Boolean {
        val cards = message.handcards
        val iam = message.youAre

        if (message.tichuMap.getValue(iam) != ETichu.NONE ||
            message.tichuMap.getValue(iam.partner) != ETichu.NONE ||
            cards.size != 14
        ) {
            return false
        }
        val (heightness, orphPenalties) = evaluateCardsBeforeSchupf(cards)
        return heightness - orphPenalties > 22
    }

    private fun evaluateSmallTichuAfterSchupf(message: CardsMessage): Boolean {
        val cards = message.handcards
        val iam = message.youAre
        if (message.tichuMap.getValue(iam) != ETichu.NONE ||
            message.tichuMap.getValue(iam.partner) != ETichu.NONE ||
            cards.size != 14
        ) {
            return false
        }
        val (heightness, orphPenalties) = evaluateCardsAfterSchupf(cards)
        return heightness - orphPenalties > 18
    }

    private fun evaluateCardsBeforeSchupf(cards: List<HandCard>): Pair<Double, Double> {
        var heightness = cards.map {
            when (it) {
                // norming to
                is NumberCard -> 2 * normValue(it).pow(2)
                PHX, DRG -> 5.0
                DOG -> 2.0
                else -> 0.0
            }
        }.sum()
        val (pats, orphs) = _allPatterns(cards, false)
        val bomb = pats.any { it is ch.taburett.tichu.patterns.Bomb || it is BombStraight }
        if (bomb) {
            heightness += 7.0
        }
        val orphPenalties = orphs.map {
            when (it.card) {
                DOG, DRG -> 0.0
                else -> (1 - normValue(it.card)).pow(3)
            }
        }.sum()
        return Pair(heightness, orphPenalties)
    }

    private fun evaluateCardsAfterSchupf(cards: List<HandCard>): Pair<Double, Double> {
        var heightness = cards.map {
            when (it) {
                // norming to
                is NumberCard -> 2 * normValue(it).pow(2)
                PHX, DRG -> 5.0
                DOG -> -3.0
                else -> 0.0
            }
        }.sum()

        val (pats, orphs) = _allPatterns(cards, false)
        val bomb = pats.any { it is ch.taburett.tichu.patterns.Bomb || it is BombStraight }
        if (bomb) {
            heightness += 7.0
        }
        val orphPenalties = orphs.map {
            when (it.card) {
                DRG -> -1.0
                DOG -> 1.0
                else -> (1 - normValue(it.card)).pow(3)
            }
        }.sum()
        return Pair(heightness, orphPenalties)
    }


}

