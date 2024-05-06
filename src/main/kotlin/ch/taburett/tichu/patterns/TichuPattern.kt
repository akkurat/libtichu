package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.LegalityAnswer
import ch.taburett.tichu.game.message
import ch.taburett.tichu.game.ok

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
                if (other.type == TichuPatternType.BOMBSTRAIGHT) {
                    return message("can't beat a straight bomb with a normal bomd")
                } else {
                    // no bomb int other (types are !equal if we land here)
                    return ok()
                }
            }
            if (type == TichuPatternType.BOMBSTRAIGHT) {
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