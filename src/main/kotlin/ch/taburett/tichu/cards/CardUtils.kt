package ch.taburett.tichu.cards

import ch.taburett.tichu.game.LegalityAnswer
import ch.taburett.tichu.game.message
import ch.taburett.tichu.game.ok


fun count(cards: Iterable<HandCard>): Int {
    return cards.sumOf { c -> c.points() }
}

//fun validMove(tableCards: Set<ch.taburett.tichu.cards.PlayCard>, toPlayCards: Set<ch.taburett.tichu.cards.PlayCard>): Boolean {
//
//}


fun pattern(cards: Collection<PlayCard>): TichuPattern {
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


enum class TichuPatternType(val factory: PatternImplFactory) {
    EMPTY(Empty),
    SINGLDE(Single),
    PAIR(TichuPair),
    TRIPLE(TichuTriple),
    BOMB(Bomb),
    FULLHOUSE(FullHouse),
    STAIRS(Stairs),
    RUELLE(Ruelle),
    RUELBOMB(RuelleBomb);

    fun pattern(cards: Collection<PlayCard>): TichuPattern? {
        return factory.pattern(cards);
    }

    fun patterns(cards: Collection<HandCard>): Set<TichuPattern> {
        return factory.allPatterns(cards)
    }


}

interface PatternImplFactory {
    fun pattern(cards: Collection<PlayCard>): TichuPattern?
    fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern>
}

fun allSameValue(cards: Collection<PlayCard>): Int? {
    val valueToMatch = cards.first().value()
    if (cards.all { c -> c.value() == valueToMatch }) {
        return valueToMatch
    }
    return null
}


abstract class TichuPattern(val type: TichuPatternType, cards: Iterable<PlayCard>) {
    val cards: Set<PlayCard>

    init {
        this.cards = cards.toSet()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TichuPattern) return false

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

    fun cardinality(): Int {
        return cards.size;
    }

    fun rank(): Int {
        return cards.maxOf { c -> c.value() }
    }

    fun beats(other: TichuPattern): LegalityAnswer {
        if (type != other.type) {
            if (other.type == TichuPatternType.EMPTY) {
                return ok()
            }
            if (type == TichuPatternType.BOMB) {
                if (other.type == TichuPatternType.RUELBOMB) {
                    return message("can't beat a straight bomb with a normal bomd")
                } else {
                    // no bomb int other (types are !equal if we land here)
                    return ok()
                }
            }
            if (type == TichuPatternType.RUELBOMB) {
                /// beats everything
                return ok();
            }
            return message("incompatible types: " + type + " and " + other.type)
        }

        if (cardinality() != other.cardinality()) {

            return message("pattern differ in lenght")
        }
        if (rank() <= other.rank()) {
            return message("get higher")
        }
        return ok();

    }

}


