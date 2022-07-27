class Bomb(cards: Collection<PlayCard>) : ConcretePattern(TichuPattern.BOMB,cards) {
    companion object : PatternFactory {
        override fun pattern(cards: Collection<PlayCard>): ConcretePattern? {
            val valueToMatch = cards.first().value()
            if (cards.all { c -> c.value() == valueToMatch && c.color() != Color.SPECIAL }) {
                return Bomb(cards);
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<ConcretePattern> {
            val useableCards = cards.filterIsInstance<NumberCard>()
            val patterns = useableCards.groupBy { it.value() }.values
                .filter{ it.size == 4 }
                .map { Bomb(it) }
                .toSet()
            return patterns
        }
    }

}