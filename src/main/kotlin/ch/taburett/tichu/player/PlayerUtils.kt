package ch.taburett.tichu.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.protocol.Message.*
import ch.taburett.tichu.game.core.gameplay.wishPredicate
import ch.taburett.tichu.patterns.Bomb
import ch.taburett.tichu.patterns.BombStraight
import ch.taburett.tichu.patterns.TichuPattern
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

fun cardGettingRidIncentive(hc: HandCard): Double {
    return when (hc) {
        is NumberCard -> (1 - normValue(hc)).pow(3)
        DOG -> 1.0
        MAH -> (13/14.0).pow(3)
        else -> 0.0
    }
}

fun normValue(hc: PlayCard) = hc.getValue() / 14

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

// dep. on the played cards also the pattern values change
fun patPenalty(pat: TichuPattern): Double {
    return when (pat.type) {
        ANSPIEL -> 0.0
        SINGLE -> 200.0
        PAIR -> 100.0
        TRIPLE -> 50.0
        FULLHOUSE -> 30.0
        STRAIGHT -> 30.0
        STAIRS -> 10.0
        BOMB -> 0.0
        BOMBSTRAIGHT -> 0.0
    }
}

fun mapPattern(pat: TichuPattern): Double {
    return when (pat) {
        is Bomb, is BombStraight ->
            pat.cards.sumOf { c -> patValue(pat) }

        else ->
            pat.cards.sumOf { c -> cardCost(c) * patValue(pat) }
    }
}

fun penaltyPattern(pat: TichuPattern): Double {
    return when (pat) {
        is Bomb, is BombStraight ->
            pat.cards.sumOf { c -> -patValue(pat) }

        else ->
            pat.cards.sumOf { c -> cardGettingRidIncentive(c) * patValue(pat) }
    }
}


fun weightPossibilitesNoRec(handcards: Collection<HandCard>): Map<TichuPattern, Double> {
    return mapPatternValues(allPatterns(handcards), handcards)
}

fun mapPatternValues(all: Set<TichuPattern>, handcards: Collection<HandCard>): Map<TichuPattern, Double> {
    val vals = all.associateWith { pat ->
        mapPattern(pat) + weightPossibilites(pat, handcards)
    }
    val norm = vals.values.sumOf { it.pow(2) }.pow(0.5)
    return vals.mapValues { it.value / norm }
}

fun weightPossibilites(
    pat: TichuPattern,
    allcards: Collection<HandCard>,
): Double {

    val rest = allcards - pat.cards
    if (rest.isEmpty()) {
        return -5.0 * allcards.size
    }


    val all = allPatterns(rest).filter { it.type != SINGLE }
    val orphans = rest - all.flatMap { it.cards }.toSet()
    val price = all.sumOf { penaltyPattern(it) } + orphans.sumOf { 5 * cardGettingRidIncentive(it) }
    return price
//
//        for (e in costMap) {
//            map.compute(e.key) { _, i -> i!! + e.value }
//        }
//        patternCost + restCost

}

fun interface PlayerMessageConsumer {
    fun accept(m: PlayerMessage)
}


fun WhosMove.probabilitiesByMessage(): Map<TichuPattern, Double> {
    val myPatterns = allPatterns(handcards).toSet()
    return probabilitiesByMessage(myPatterns)
}

fun WhosMove.probabilitiesByMessage(myPatterns: Set<TichuPattern>): Map<TichuPattern, Double> {
    return prob(
        handcards, goneCards,
        myPatterns, cardCounts.getValue(this.youAre.partner),
        cardCounts.getValue(this.youAre.li),
        cardCounts.getValue(this.youAre.re)
    )
}