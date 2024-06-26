package ch.taburett.tichu.botplayer

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.TichuPattern
import ch.taburett.tichu.patterns.TichuPatternType.SINGLE

fun prob(
    handcards: List<HandCard>,
    goneCards: Collection<PlayCard>,
    myPatterns: Set<TichuPattern>,
    nPartner: Int,
    nRe: Int,
    nLi: Int,
): Map<TichuPattern, Double> {
    val restcards = fulldeck - handcards - goneCards

    val out = myPatterns.associateWith { 0.0 }.toMutableMap()

    for (i in 0..<1000) {
        val patterns = extracted(myPatterns, restcards, nPartner, nRe, nLi)
        for (p in patterns.filterValues { it }) {
            out[p.key] = out[p.key]!!.inc()
        }

    }
    return out.mapValues { it.value / 1000.0 }
}

private fun extracted(
    myPatterns: Set<TichuPattern>,
    restcards: Collection<HandCard>,
    nPartner: Int,
    nRe: Int,
    nLi: Int,
): Map<TichuPattern, Boolean> {
    val (partner, left, right) = randomCards(restcards, nPartner, nRe, nLi)

    partner
    val counterTeam = myPatterns.filter { it.type !=SINGLE }
        .associateWith { it.findBeatingPatterns(left).isNotEmpty() || it.findBeatingPatterns(right).isNotEmpty() }

    val counter = (left+right ).filterIsInstance<NumberCard>().toSet()
    val singlesCounter = myPatterns.filterIsInstance<Single>()
        .associateWith { p -> counter.any{ it.getValue() > p.card.getValue() } }


    return counterTeam + singlesCounter

//    val rightPatterns = myPatterns.filter { it.type != SINGLE }.associateWith { it.findBeatingPatterns(right) }


}

fun randomCards(
    restcards: Collection<HandCard>,
    nPartner: Int,
    nRe: Int,
    nLi: Int,
): Triple<List<HandCard>, List<HandCard>, List<HandCard>> {
    val shuffled = restcards.shuffled()

    assert(nPartner + nRe + nLi == restcards.size)

    val partner = shuffled.take(nPartner)
    val left = shuffled.drop(nPartner).take(nLi)
    val right = shuffled.drop(nPartner + nLi).take(nRe)
    return Triple(partner, left, right)
}