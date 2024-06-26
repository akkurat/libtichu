package ch.taburett.tichu.cards

import ch.taburett.tichu.patterns.TichuPattern
import ch.taburett.tichu.patterns.TichuPatternType

fun fulldeckAsPlayCards(phx: Double): List<PlayCard> {
    return fulldeck.filterIsInstance<PlayCard>() + PHX.asPlayCard(phx)
}

fun pattern(cards: Collection<PlayCard >): TichuPattern {
    for (pattern in TichuPatternType.values()) {
        val cPattern = pattern.pattern(cards);
        if (cPattern != null) {
            return cPattern
        }
    }
    throw IllegalArgumentException("No Pattern matched")
}

fun allPatterns(cards: Collection<HandCard>, incPhx: Boolean = true): Set<TichuPattern> {
    return TichuPatternType.entries
        .flatMap { it.allPatterns(cards, incPhx = incPhx) }
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



fun parsePlayCard(code: String): PlayCard {
    if (code.startsWith(PHX.getCode(), true)) {
        val rank = code.substring(3).toDouble()
        return PHX.asPlayCard(rank);
    } else {
        return lookupByCode.getValue(code) as PlayCard
    }
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
