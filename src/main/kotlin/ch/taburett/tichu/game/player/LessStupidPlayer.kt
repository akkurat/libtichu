package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.prob
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.Single
import java.util.*
import java.util.function.Consumer


fun lessStupidMove(message: ServerMessage, listener: Consumer<PlayerMessage>, player: Player) {
    when (message) {
        is AckGameStage -> ack(message, listener)
        is Schupf -> listener.accept(Ack.SchupfcardReceived())
        is WhosMove -> move(message, listener, message.wish, player)
        else -> {}
    }
}

private fun move(
    wh: WhosMove,
    listener: Consumer<PlayerMessage>,
    wish: Int?,
    player: Player,
) {
    if (wh.stage == Stage.GIFT_DRAGON) {
        listener.accept(GiftDragon(GiftDragon.ReLi.LI))
    } else if (wh.stage == Stage.YOURTURN) {
        val table = wh.table
        val handcards = wh.handcards
        val weights = weightPossibilites(handcards)
        val allPatterns = allPatterns(handcards)

        val me = wh.youAre
        val probs = prob( handcards, wh.goneCards, wh.cardCounts.get(me.partner())!!, wh.cardCounts.get(me.li())!!,
            wh.cardCounts.get(me.re())!!
        )

        // todo: take cheapest and most unlike to get beaten
        // table is empty. try to play least valueable cards
        val patternPrice = allPatterns
            .associateWith { p -> p.cards.sumOf { c -> weights.getValue(c) } / p.cards.size * (probs.getValue(p)+0.5) }

        val cheapest = patternPrice.minBy { it.value }
        if (table.isEmpty()) {
            val mightFullfillWish = mightFullfillWish(handcards, wish)
            if (mightFullfillWish) {
                val numberCard = handcards
                    .filterIsInstance<NumberCard>()
                    .filter { nc -> Objects.equals(wh.wish, nc.getValue()) }
                    .minByOrNull { it.getValue() }
                listener.accept(moveSingle(numberCard))
                return
            }

            val cards = cheapest.key.cards


            // play smallest pattern

            val values = handcards.filterIsInstance<NumberCard>()
                .map { it.getValue() }
                .toSet()

            val rw = (2..15).filter { n -> !values.contains(n.toDouble()) }
            val randomWish: Int = rw.shuffled().first()

            val move: Move
            if (cards.contains(MAH)) {
                move = move(cards, randomWish)
            } else {
                move = move(cards)
            }
            listener.accept(move)

        } else {

            val toBeat = table.toBeat()
            val pat = pattern(toBeat.cards)

            var beatingPatterns = pat.findBeatingPatterns(handcards).toMutableList()
            val beatingPrice = beatingPatterns
                .associateWith { p -> p.cards.sumOf { c -> weights.getValue(c) } }


            if (wh.wish != null) {
                var allWithWish = beatingPatterns
                    .filter { p -> p.cards.any { c -> (c.getValue() - wh.wish) == 0.0 } }
                    .toMutableList()
                if (!allWithWish.isEmpty()) {
                    beatingPatterns = allWithWish
                }
            }


            if (pat is Single) {
                if (handcards.contains(DRG)) {
                    beatingPatterns.add(Single(DRG))
                }
                if (handcards.contains(PHX)) {
                    beatingPatterns.add(Single(PHX.asPlayCard(pat.card.getValue() + 1)))
                }
            }
            // todo: if cheapestbreating cheaper than cheapest at all?
            // take into account cards of other players
            val beatingCheapest = beatingPrice.minByOrNull { it.value }

            if (beatingCheapest != null && beatingCheapest.value <=  cheapest.value) {
                if (
                    toBeat.player.playerGroup != player.playerGroup ||
                    pat.rank() < 10
                ) {
                    listener.accept(move(beatingCheapest.key.cards))
                } else {
                    listener.accept(move(listOf()))
                }
            } else {
                listener.accept(move(listOf()))
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
