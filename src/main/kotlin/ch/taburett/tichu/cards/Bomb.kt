package ch.taburett.tichu.cards

class Bomb(cards: Collection<PlayCard>) : TichuPattern(TichuPatternType.BOMB,cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            val valueToMatch = cards.first().value()
            if (cards.all { c -> c.value() == valueToMatch && c.color() != Color.SPECIAL }) {
                return Bomb(cards);
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            val useableCards = cards.filterIsInstance<NumberCard>()
            val patterns = useableCards.groupBy { it.value() }.values
                .filter{ it.size == 4 }
                .map { Bomb(it) }
                .toSet()
            return patterns
        }
    }

}