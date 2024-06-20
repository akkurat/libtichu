package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.Single


class StupidPlayer(val listener: (PlayerMessage) -> Unit) : BattleRound.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: Player) {
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
            is WhosMove -> ch.taburett.tichu.game.player.stupidMove(message)
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
            move(listOf())
        } else {
            val mypat = all.sortedBy { it.rank() }.take(4).randomOrNull()
            if (mypat != null) move(mypat.cards) else move(listOf())
        }
    }

    return if (evaluateSmallTichu(m.handcards)) {
        listOf(SmallTichu(), move)
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
                move(listOf())
            }
        }
    }
    return if (evaluateSmallTichu(message.handcards)) {
        listOf(SmallTichu(), move)
    } else {
        listOf(move)
    }
}

private fun ack(
    message: AckGameStage,
): PlayerMessage? {
    return when (message.stage) {
        Stage.EIGHT_CARDS -> {
            if (evaluateBigTichu(message.cards)) {
                BigTichu()
            } else {
                Ack.BigTichu()
            }
        }

        Stage.PRE_SCHUPF -> {
            if (evaluateSmallTichu(message.cards)) {
                SmallTichu()
            } else {
                Ack.TichuBeforeSchupf()
            }
        }

        Stage.SCHUPF -> {
            val cards = message.cards.sorted()
            Schupf(cards.get(0), cards.get(1), cards.last())
        }

        Stage.POST_SCHUPF -> {
            if (evaluateSmallTichu(message.cards)) {
                SmallTichu()
            } else {
                Ack.TichuBeforePlay()
            }
        }

        else -> {
            null
        }
    }
}

fun evaluateSmallTichu(cards: List<HandCard>): Boolean {
    if (cards.size != 14) {
        return false
    }
    return (cards.contains(PHX) || cards.contains(DRG)) &&
            cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 2
            ||
            (cards.contains(PHX) && cards.contains(DRG)) &&
            cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 1
}

fun evaluateBigTichu(cards: List<HandCard>): Boolean {
    return cards.containsAll(listOf(PHX, DRG))
            && cards.filterIsInstance<NumberCard>().count { it.getValue() == 14.0 } >= 2

}
