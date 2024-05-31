package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType.OK
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import java.util.*
import java.util.List
import java.util.function.Consumer


fun stupidMove(message: ServerMessage, listener: Consumer<PlayerMessage>, player: Player) {
    when (message) {
        is AckGameStage -> ack(message, listener)
        is Schupf -> listener.accept(Ack.SchupfcardReceived())
        is WhosMove -> move(message, listener, message.wish, player)
        else -> {}
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
            val mightFullfillWish = message.handcards
                .filterIsInstance<NumberCard>()
                .any(wishPredicate(wish))
            if (mightFullfillWish) {
                val numberCard = message.handcards
                    .filterIsInstance<NumberCard>()
                    .filter { nc -> Objects.equals(message.wish, nc.getValue()) }
                    .minByOrNull { it.getValue() }
                listener.accept(Move(numberCard))
                return
            }





            // play smallest card
            var ocard = handcards.stream()
                .min(Comparator.comparingDouble(HandCard::getSort))
                .orElse(null)

            var values = handcards.filterIsInstance<NumberCard>()
                .map { h -> h.getValue() }
                .toSet()

            var rw = (2..15).filter { n -> !values.contains(n.toDouble()) }
            val randomWish: Int = rw.shuffled().first()

            var move: Move
            if (ocard is Phoenix) {
                move = Move(List.of(ocard.asPlayCard(1)))
            } else if (ocard == MAH) {
                move = Move(List.of(MAH), randomWish)
            } else if (ocard is PlayCard) {
                move = Move(List.of(ocard))
            } else {
                move = Move(List.of())
            }

            listener.accept(move)

        } else {
            var toBeat = table.toBeat()
            var pat = pattern(toBeat.cards)
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
                if (handcards.contains(PHX)) {
                    all.add(Single(PHX.asPlayCard(pat.card.getValue() + 1)))
                }
            }
            if (all.isEmpty()) {
                listener.accept(Move(List.of()))
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
                        listener.accept(Move(mypat.get().cards))
                    } else {
                        listener.accept(Move(List.of()))
                    }
                } else {
                    listener.accept(Move(List.of()))
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
