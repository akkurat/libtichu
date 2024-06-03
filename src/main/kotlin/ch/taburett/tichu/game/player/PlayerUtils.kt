package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.wishPredicate
import ch.taburett.tichu.patterns.*
import ch.taburett.tichu.patterns.TichuPatternType.*
import kotlin.math.pow

fun mightFullfillWish(handcards: List<HandCard>, wish: Int?) = handcards
    .filterIsInstance<NumberCard>()
    .any(wishPredicate(wish))

// basically dep. on the game the card value changes
fun cardCost(hc: HandCard): Double {
    return when (hc) {
        // norming to
        is NumberCard -> 2 * normValue(hc).pow(2)
        PHX -> 3.0
        DRG -> 3.0
        else -> 0.0
    }
}

fun cardPenalty(hc: HandCard): Double {
    return when (hc) {
        is NumberCard -> (1 - normValue(hc)).pow(2)
        else -> 0.0
    }
}

private fun normValue(hc: NumberCard) = (hc.getValue() - 2) / 12

// dep. on the played cards also the pattern values change
fun patValue(pat: TichuPattern): Double {
    return when (pat.type) {
        ANSPIEL -> 0.0
        SINGLE -> 1.0
        PAIR -> 2.0
        TRIPLE -> 3.0
        FULLHOUSE -> 5.0
        STRAIGHT -> 5.0
        STAIRS -> 10.0
        BOMB -> 100.0
        BOMBSTRAIGHT -> 200.0
    }
}

fun mapPattern(pat: TichuPattern): Map<PlayCard, Double> {
    return when (pat) {
        is Bomb, is BombStraight ->
            pat.cards.associateWith { c -> patValue(pat) }

        is FullHouse ->
            pat.three.associateWith { cardCost(it) * 4 } + pat.two.associateWith { cardPenalty(it) * 4 }

        else ->
            pat.cards.associateWith { c -> cardCost(c) * patValue(pat) }
    }
}

fun weightPossibilites(handcards: List<HandCard>, damping: Double = 1.0): Map<HandCard, Double> {
    val all = allPatterns(handcards).filter { it !is Single }
    if (all.isEmpty()) {
        return handcards.associateWith { cardCost(it) }
    }
    val map: MutableMap<HandCard, Double> = handcards
        .associateWith { cardCost(it) }
        .toSortedMap(compareBy { it.getSort() })

    for (pat in all) {

        val costMap = mapPattern(pat)

        for (e in costMap) {
            map.compute(e.key) { _, i -> i!! + e.value }
        }

        val rest = handcards - pat.cards

        for (hc in rest) {
            map.compute(hc) { c, i -> i!! + cardPenalty(c) }
        }

        val weights = weightPossibilites(rest, 0.8*damping)

        for (w in weights) map.compute(w.key) { _, i -> i!! + w.value*damping }
    }
    return map
}

