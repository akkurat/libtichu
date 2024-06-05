package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PHX
import ch.taburett.tichu.cards.PlayCard

class Single(val card: PlayCard) : TichuPattern(TichuPatternType.SINGLE, setOf(card)) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.size == 1) {
                return Single(cards.first())
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>, cardinality: Int?): Set<Single> {
            // todo: phx?
            val s = cards.filterIsInstance<PlayCard>()
                .map { Single(it) }.toMutableList()
            if (cards.contains(PHX)) {
                s.add(Single(PHX.asPlayCard(1.5)))
            }
            return s.toSet()
        }
    }

}