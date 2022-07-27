package ch.taburett.tichu

fun count(cards: Iterable<HandCard>): Int {
    return cards.sumOf { c -> c.points() }
}

//fun validMove(tableCards: Set<ch.taburett.tichu.PlayCard>, toPlayCards: Set<ch.taburett.tichu.PlayCard>): Boolean {
//
//}


fun pattern(cards: Collection<PlayCard>): ConcretePattern {
    for (pattern in TichuPattern.values()) {
        val cPattern = pattern.pattern(cards);
        if (cPattern != null) {
            return cPattern
        }
    }
    throw IllegalArgumentException("No Pattern matched")
}

fun allPatterns(cards: Collection<HandCard>): Set<ConcretePattern> {
    return TichuPattern.values()
        .flatMap { it.patterns(cards) }
        .toSet()
}


enum class TichuPattern(val factory: PatternFactory) {
    SINGLDE(Single),
    PAIR(TichuPair),
    TRIPLE(TichuTriple),
    BOMB(Bomb),
    FULLHOUSE(FullHouse),
    STAIRS(Stairs),
    RUELLE(Ruelle),
    RUELBOMB(RuelleBomb);

    fun pattern(cards: Collection<PlayCard>): ConcretePattern? {
        return factory.pattern(cards);
    }

    fun patterns(cards: Collection<HandCard>): Set<ConcretePattern> {
        return factory.allPatterns(cards)
    }


}

interface PatternFactory {
    fun pattern(cards: Collection<PlayCard>): ConcretePattern?
    fun allPatterns(cards: Collection<HandCard>): Set<ConcretePattern>
}

fun allSameValue(cards: Collection<PlayCard>): Int? {
    val valueToMatch = cards.first().value()
    if( cards.all { c -> c.value() == valueToMatch } )
    {
        return valueToMatch
    }
    return null
}


abstract class ConcretePattern(val type: TichuPattern, cards: Iterable<PlayCard>) {
    val cards: Set<PlayCard>

    init {
        this.cards = cards.toSet()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConcretePattern) return false

        if (type != other.type) return false
        if (cards != other.cards) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + cards.hashCode()
        return result
    }

    override fun toString(): String {
        return type.toString() + cards.toString()
    }

}


