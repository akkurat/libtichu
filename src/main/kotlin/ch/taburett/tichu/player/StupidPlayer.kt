package ch.taburett.tichu.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.core.common.ETichu
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.communication.CardsMessage
import ch.taburett.tichu.game.communication.Message.*
import ch.taburett.tichu.game.communication.createMove
import ch.taburett.tichu.game.communication.moveSingle
import ch.taburett.tichu.patterns.Single
import java.util.function.Consumer


class StupidPlayer(val listener: (PlayerMessage) -> Unit) : BattleRound.AutoPlayer {
    constructor(listener: Consumer<PlayerMessage>) : this({ listener.accept(it) }) {

    }


    override fun receiveMessage(message: ServerMessage, player: EPlayer) {
        val moves = stupidMove(message)
        moves.forEach { listener(it) }
    }

    override fun toString(): String {
        return type
    }

    override val type: String = "Stupid"


    internal fun stupidMove(message: ServerMessage): List<PlayerMessage> {
        return when (message) {
            is AckGameStage -> {
                val ack = ack(message)
                ack?.let { listOf(it) } ?: listOf()
            }

            is Schupf -> listOf(Ack.SchupfcardReceived())
            is WhosMove -> ch.taburett.tichu.player.stupidMove(message)
            else -> listOf()
        }
    }
}


fun stupidMove(
    message: WhosMove,
): List<PlayerMessage> = when (message.stage) {
    Stage.GIFT_DRAGON -> {
        listOf(GiftDragon(GiftDragon.ReLi.LI))
    }

    Stage.YOURTURN -> {
        if (message.table.isEmpty()) {
            opening(message)
        } else {
            response(message)
            // pattern
        }
    }

    else -> listOf()
}

private fun response(
    m: WhosMove,
): List<PlayerMessage> {
    val move = with(m) {
        val pat = pattern(table.toBeatCards())
        var all = pat.findBeatingPatterns(handcards).toMutableList()

        if (m.wish != null) {
            val allWithWish = all
                .filter { p -> p.cards.any { c -> (c.getValue() - m.wish) == 0.0 } }
                .toMutableList()
            if (allWithWish.isNotEmpty()) {
                all = allWithWish
            }
        }

        if (pat is Single) {
            if (handcards.contains(DRG)) {
                all.add(Single(DRG))
            }
            if (pat.card != DRG && handcards.contains(PHX)) {
                all.add(Single(PHX.asPlayCard(pat.card.getValue() + .5)))
            }
        }
        if (all.isEmpty()) {
            createMove(listOf())
        } else {
            val mypat = all.sortedBy { it.rank() }.take(4).randomOrNull()
            if (mypat != null) createMove(mypat.cards) else createMove(listOf())
        }
    }

    return if (evaluateSmallTichu(m)) {
        listOf(Announce.SmallTichu(), move)
    } else {
        listOf(move)
    }

}

private fun opening(
    message: WhosMove,
): List<PlayerMessage> {
    val move = with(message) {
        val mightFullfillWish = mightFullfillWish(handcards, message.wish)
        if (mightFullfillWish) {
            val numberCard = handcards
                .filterIsInstance<NumberCard>()
                .filter { nc -> message.wish!! - nc.getValue() == 0.0 }
                .minByOrNull { it.getValue() }
            moveSingle(numberCard)
        }

        // play smallest card
        val ocard = handcards.minBy { it.getSort() }

        when (ocard) {
            is Phoenix -> {
                moveSingle(ocard.asPlayCard(1))
            }

            is PlayCard -> {
                moveSingle(ocard)
            }

            else -> {
                createMove(listOf())
            }
        }
    }
    return if (evaluateSmallTichu(message)) {
        listOf(Announce.SmallTichu(), move)
    } else {
        listOf(move)
    }
}

private fun ack(
    message: AckGameStage,
): PlayerMessage? {
    return when (message.stage) {
        Stage.EIGHT_CARDS -> {
            if (evaluateBigTichu(message.handcards)) {
                Announce.BigTichu()
            } else {
                Ack.BigTichu()
            }
        }

        Stage.PRE_SCHUPF -> {
            if (evaluateSmallTichu(message)) {
                Announce.SmallTichu()
            } else {
                Ack.TichuBeforeSchupf()
            }
        }

        Stage.SCHUPF -> {
            val cards = message.handcards.sorted()
            Schupf(cards.get(0), cards.get(1), cards.last())
        }

        Stage.POST_SCHUPF -> {
            if (evaluateSmallTichu(message)) {
                Announce.SmallTichu()
            } else {
                Ack.TichuBeforePlay()
            }
        }

        else -> {
            null
        }
    }
}

private fun evaluateSmallTichu(message: CardsMessage): Boolean {
    val cards: List<HandCard> = message.handcards
    val iam = message.youAre

    if (message.tichuMap.getValue(iam) != ETichu.NONE ||
        message.tichuMap.getValue(iam.partner) != ETichu.NONE ||
        cards.size != 14
    ) {
        return false
    }
    return (cards.contains(PHX) || cards.contains(DRG)) &&
            cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 2
            ||
            (cards.contains(PHX) && cards.contains(DRG)) &&
            cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 1
}

private fun evaluateBigTichu(cards: List<HandCard>): Boolean {
    val bigTichu = (cards.containsAll(listOf(PHX, DRG))
            && cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 1 )||

            (cards.contains(PHX) || cards.contains(DRG)) &&
                    cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 2
    if (bigTichu) println("Stupid: bigTichu")
    return bigTichu

}
