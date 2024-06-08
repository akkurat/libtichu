package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType.OK
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import java.util.*


class StupidPlayer(val listener: (PlayerMessage) -> Unit) : Round.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: Player) {
        val move = stupidMove(message)
        if(move != null) listener(move)
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
        val table = message.table
        val handcards = message.handcards
        if (table.isEmpty()) {
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
        } else {
            val toBeat = table.toBeat()
            val pat = pattern(toBeat.cards)
            var all = pat.findBeatingPatterns(handcards).toMutableList()

            if (message.wish != null) {
                val allWithWish = all
                    .filter { p -> p.cards.any { c -> (c.getValue() - message.wish) == 0.0 } }
                    .toMutableList()
                if (!allWithWish.isEmpty()) {
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
                val mypat = all.stream()
                    .filter { p -> p.beats(pat).type == OK }
                    .min(Comparator.comparingDouble(TichuPattern::rank))
                if (mypat.isPresent) {
                    move(mypat.get().cards)
                } else {
                    move(listOf())
                }
            }
            // pattern
        }
    }
    return null
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
