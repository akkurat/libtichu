package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard

class Empty : TichuPattern(TichuPatternType.ANSPIEL, setOf()) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.isEmpty()) {
                return Empty()
            }
            return null
        }
        // puh.. in theory this should return all possible pattern
        // maybe it's better not to have Empty pattern at all
        override fun allPatterns(cards: Collection<HandCard>, cardinality: Int?): Set<TichuPattern> {
            return setOf()
        }
    }

}