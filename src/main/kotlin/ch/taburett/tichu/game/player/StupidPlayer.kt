package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.Single
import java.util.*


class StupidPlayer(val listener: (PlayerMessage) -> Unit) : Round.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: Player) {
        val move = stupidMove(message)
        if (move != null) listener(move)
    }

    override fun toString(): String {
        return type
    }

    override val type: String = "Stupid"


    internal fun stupidMove(message: ServerMessage): PlayerMessage? {
        return when (message) {
            is AckGameStage -> ack(message)
            is Schupf -> Ack.SchupfcardReceived()
            is WhosMove -> ch.taburett.tichu.game.player.stupidMove(message)
//            is Rejected -> println(this)
            else -> null
        }
    }
}


fun stupidMove(
    message: WhosMove,
): PlayerMessage? {
    if (message.stage == Stage.GIFT_DRAGON) {
        return GiftDragon(GiftDragon.ReLi.LI)
    } else if (message.stage == Stage.YOURTURN) {
        if (message.table.isEmpty()) {
            return opening(message)
        } else {
            return response(message)
            // pattern
        }
    }
    return null
}

private fun response(
    m: WhosMove,
): PlayerMessage? {
    with(m) {
        val toBeat = table.toBeat()
        val pat = pattern(toBeat.cards)
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
        return if (all.isEmpty()) {
            move(listOf())
        } else {
            val mypat = all.sortedBy { it.rank() }.take(4).randomOrNull()
            if (mypat != null) move(mypat.cards) else move(listOf())
        }
    }
}

private fun opening(
    message: WhosMove,
): PlayerMessage {
    with(message) {
        val mightFullfillWish = mightFullfillWish(handcards, message.wish)
        if (mightFullfillWish) {
            val numberCard = handcards
                .filterIsInstance<NumberCard>()
                .filter { nc -> Objects.equals(message.wish, nc.getValue()) }
                .minByOrNull { it.getValue() }
            return moveSingle(numberCard)
        }

        // play smallest card
        val ocard = handcards.minBy { it.getSort() }

        return when (ocard) {
            is Phoenix -> {
                moveSingle(ocard.asPlayCard(1))
            }

            is PlayCard -> {
                moveSingle(ocard)
            }

            else -> {
                move(listOf())
            }
        }
    }
}

private fun ack(
    message: AckGameStage,
): PlayerMessage? {
    return when (message.stage) {
        Stage.EIGHT_CARDS -> Ack.BigTichu()
        Stage.PRE_SCHUPF -> Ack.TichuBeforeSchupf()
        Stage.SCHUPF -> {
            val cards = message.cards
            Schupf(cards.get(0), cards.get(1), cards.get(2))
        }

        Stage.POST_SCHUPF -> Ack.TichuBeforePlay()
        else -> null
    }
}
