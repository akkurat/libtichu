package ch.taburett.tichu.cards

import ch.taburett.tichu.patterns.TichuPattern
import ch.taburett.tichu.patterns.TichuPatternType

val lDRG = listOf(DRG)

fun count(cards: Iterable<HandCard>): Int {
    return cards.sumOf { c -> c.getPoints() }
}

//fun validMove(tableCards: Set<ch.taburett.tichu.cards.PlayCard>, toPlayCards: Set<ch.taburett.tichu.cards.PlayCard>): Boolean {
//
//}


fun pattern(cards: Collection<PlayCard >): TichuPattern {
    for (pattern in TichuPatternType.values()) {
        val cPattern = pattern.pattern(cards);
        if (cPattern != null) {
            return cPattern
        }
    }
    throw IllegalArgumentException("No Pattern matched")
}

fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
    return TichuPatternType.values()
        .flatMap { it.patterns(cards) }
        .toSet()
}


fun allSameValue(cards: Collection<PlayCard>): Int? {
    if(cards.size > 1) {
        val valueToMatch = cards.first().getValue()
        if (cards.all { c -> c.getValue() == valueToMatch }) {
            return valueToMatch.toInt()
        }
    }
    return null
}


fun parseCards(cardsString: String): Set<HandCard> {
    return cardsString.split(",")
        .map { c -> parseCard(c) }
        .filterNotNull()
        .toSet()
}

fun parseCard(cardString: String): HandCard? {
    if (cardString.matches(Regex.fromLiteral("PHX\\d+"))) {
        val value = cardString.substring("PHX".length).toDouble()
        return PHX.asPlayCard(value)
    }
    return lookupByName.get(cardString)
}
