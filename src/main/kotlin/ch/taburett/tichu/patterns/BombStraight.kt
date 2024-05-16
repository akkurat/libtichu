package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard

class BombStraight(cards: Collection<PlayCard>) : TichuPattern(TichuPatternType.BOMBSTRAIGHT, cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (Straight.isValidRuelle(cards)) {
                if (cards.all { it.getColor() == cards.first().getColor() }) {
                    return BombStraight(cards)
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            TODO("Not yet implemented")
        }
    }

}