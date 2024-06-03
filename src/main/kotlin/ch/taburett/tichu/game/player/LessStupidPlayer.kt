package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.prob
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import java.util.*
import java.util.function.Consumer


fun lessStupidMove(message: ServerMessage, listener: Consumer<PlayerMessage>, player: Player) {
    when (message) {
        is AckGameStage -> ack(message, listener)
        is Schupf -> listener.accept(Ack.SchupfcardReceived())
        is WhosMove -> move(message, listener)
        else -> {}
    }
}

private fun move(
    wh: WhosMove,
    listener: Consumer<PlayerMessage>,
) {
    if (wh.stage == Stage.GIFT_DRAGON) {
        listener.accept(GiftDragon(GiftDragon.ReLi.LI))
    } else if (wh.stage == Stage.YOURTURN) {
        val weights = weightPossibilites(wh.handcards)
        val probs = probabilitiesByMessage(wh)

        val move =
            if (wh.table.isEmpty()) {
                openingMove(wh)
            } else {
                reactionMove(wh)
            }

        listener.accept(move)
    }
}

private fun probabilitiesByMessage(wh: WhosMove): Map<TichuPattern, Double> = prob(
    wh.handcards,
    wh.goneCards,
    wh.cardCounts.getValue(wh.youAre.partner),
    wh.cardCounts.getValue(wh.youAre.li),
    wh.cardCounts.getValue(wh.youAre.re)
)

private fun reactionMove(
    wh: WhosMove,
): Move {
    val table = wh.table
    val handcards = wh.handcards
    val toBeat = table.toBeat()
    val pat = pattern(toBeat.cards)

    var beatingPatterns = pat.findBeatingPatterns(handcards).toMutableList()


    val beatingPrice = beatingPatterns
        .associateWith { p -> p.cards.sumOf { c -> weights.getValue(c) } }


    if (wh.wish != null) {
        val allWithWish = beatingPatterns
            .filter { p -> p.cards.any { c -> (c.getValue() - wh.wish) == 0.0 } }
            .toMutableList()
        if (allWithWish.isNotEmpty()) {
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

    val cheapest = patternPrice.minBy { it.value }

    val beatingCheapest = beatingPrice.minByOrNull { it.value }

    return if (beatingCheapest != null && beatingCheapest.value <= cheapest.value) {
        if (
            toBeat.player.playerGroup != wh.youAre.playerGroup ||
            pat.rank() < 10
        ) {
            move(beatingCheapest.key.cards)
        } else {
            move(listOf())
        }
    } else {
        move(listOf())
    }
}

private fun openingMove(
    wh: WhosMove,
): Move {

    val handcards = wh.handcards
    val mightFullfillWish = mightFullfillWish(handcards, wh.wish)
    val move =
        if (mightFullfillWish) {
            // todo: might try to play normal pattern as well
            val numberCard = handcards
                .filterIsInstance<NumberCard>()
                .filter { nc -> Objects.equals(wh.wish, nc.getValue()) }
                .minByOrNull { it.getValue() }
            moveSingle(numberCard)
        } else {
            val prob = probabilitiesByMessage(wh)
            val price =

            if (cards.contains(MAH)) {
                val ownValues = handcards.filterIsInstance<NumberCard>()
                    .map { it.getValue().toInt() }
                    .toSet()
                val rw = (2..15) - ownValues
                val randomWish: Int = rw.shuffled().first()
                move(cards, randomWish)
            } else {
                move(cards)
            }
        }
    return move
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
            listener.accept(Schupf(cards[0], cards[1], cards[2]))
        }

        Stage.POST_SCHUPF -> listener.accept(Ack.TichuBeforePlay())
        else -> {}
    }
}
