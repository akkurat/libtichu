package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.Color
import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard

class Bomb(cards: Collection<PlayCard>) : TichuPattern(TichuPatternType.BOMB,cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            val valueToMatch = cards.first().getValue()
            if (cards.all { c -> c.getValue() == valueToMatch && c.getColor() != Color.SPECIAL }) {
                return Bomb(cards);
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>, cardinality: Int?): Set<TichuPattern> {
            val useableCards = cards.filterIsInstance<NumberCard>()
            val patterns = useableCards.groupBy { it.getValue() }.values
                .filter{ it.size == 4 }
                .map { Bomb(it) }
                .toSet()
            return patterns
        }
    }

}