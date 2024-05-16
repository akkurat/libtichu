package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import kotlin.math.abs

class Stairs private constructor(cards: List<PlayCard>) : TichuPattern(TichuPatternType.STAIRS, cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.size % 2 != 0) {
                return null
            }

            val sortedcards = cards.sortedBy { it.getValue() }
            val sorted = sortedcards.map { it.getValue() }
            for (i in cards.indices step 2) {
                if (sorted[i] != sorted[i + 1]) {
                    return null
                }
                if (i < cards.size - 2)
                    if (abs(sorted[i + 1] - sorted[i + 2]) != 1) {
                        return null
                    }
            }
            return Stairs(sortedcards)
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            return setOf();
        }
    }
}