package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType.OK
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import java.util.*
import java.util.function.Consumer


class StupidPlayer(val listener: (PlayerMessage) -> Unit) : Battle.AutoPlayer {
    override fun receiveMessage(message: ServerMessage, player: Player) {
        stupidMove(message, listener, player)
    }

    private fun stupidMove(message: ServerMessage, listener: Consumer<PlayerMessage>, player: Player) {
        when (message) {
            is AckGameStage -> ack(message, listener)
            is Schupf -> listener.accept(Ack.SchupfcardReceived())
            is WhosMove -> move(message, listener, message.wish, player)
            is Rejected -> println(this)
            else -> {}
        }
    }
}


private fun move(
    message: WhosMove,
    listener: Consumer<PlayerMessage>,
    wish: Int?,
    player: Player,
) {
    if (message.stage == Stage.GIFT_DRAGON) {
        listener.accept(GiftDragon(GiftDragon.ReLi.LI))
    } else if (message.stage == Stage.YOURTURN) {
        val table = message.table
        val handcards = message.handcards
        if (table.isEmpty()) {
            val mightFullfillWish = mightFullfillWish(handcards, wish)
            if (mightFullfillWish) {
                val numberCard = handcards
                    .filterIsInstance<NumberCard>()
                    .filter { nc -> Objects.equals(message.wish, nc.getValue()) }
                    .minByOrNull { it.getValue() }
                listener.accept(moveSingle(numberCard))
                return
            }


            // play smallest card
            val ocard = handcards.minBy { it.getSort() }

            val move: Move
            if (ocard is Phoenix) {
                move = moveSingle(ocard.asPlayCard(1))
            } else if (ocard is PlayCard) {
                move = moveSingle(ocard)
            } else {
                move = move(listOf())
            }
            listener.accept(move)
        } else {
            val toBeat = table.toBeat()
            val pat = pattern(toBeat.cards)
            var all = pat.findBeatingPatterns(handcards).toMutableList()

            if (message.wish != null) {
                var allWithWish = all
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
            if (all.isEmpty()) {
                listener.accept(move(listOf()))
            } else {
                var mypat = all.stream()
                    .filter { p -> p.beats(pat).type == OK }
                    .min(Comparator.comparingDouble(TichuPattern::rank))
                if (mypat.isPresent()) {
                    var prPat = mypat.get()
                    if (
                        toBeat.player.playerGroup != player.playerGroup ||
                        pat.rank() < 10
                    ) {
                        listener.accept(move(mypat.get().cards))
                    } else {
                        listener.accept(move(listOf()))
                    }
                } else {
                    listener.accept(move(listOf()))
                }
            }
            // pattern
        }
    }
}

private fun ack(
    message: AckGameStage,
    listener: Consumer<PlayerMessage>,
) {
    when (message.stage) {
        Stage.EIGHT_CARDS -> listener.accept(Ack.BigTichu())
        Stage.PRE_SCHUPF -> listener.accept(Ack.TichuBeforeSchupf())
        Stage.SCHUPF -> {
            val cards = message.cards
            listener.accept(Schupf(cards.get(0), cards.get(1), cards.get(2)))
        }

        Stage.POST_SCHUPF -> listener.accept(Ack.TichuBeforePlay())
        else -> {}
    }
}
