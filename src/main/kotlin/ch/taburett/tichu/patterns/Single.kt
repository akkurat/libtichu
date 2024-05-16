package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard

class Single(val card: PlayCard) : TichuPattern(TichuPatternType.SINGLE, setOf(card)) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.size == 1) {
                return Single(cards.first())
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            // todo: phx? drg?
            return cards.filterIsInstance<NumberCard>()
                .map { Single(it) }.toSet()
        }
    }

}