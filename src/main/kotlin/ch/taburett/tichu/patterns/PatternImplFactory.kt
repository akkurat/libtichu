package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard

interface PatternImplFactory {
    fun pattern(cards: Collection<PlayCard>): TichuPattern?
    fun allPatterns(cards: Collection<HandCard>, cardinality: Int? = null): Set<TichuPattern>
}