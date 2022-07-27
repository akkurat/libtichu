package ch.taburett.tichu

class Single(val card: PlayCard) : ConcretePattern(TichuPattern.SINGLDE, setOf(card)) {
    companion object : PatternFactory {
        override fun pattern(cards: Collection<PlayCard>): ConcretePattern? {
            if (cards.size == 1) {
                return Single(cards.first())
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<ConcretePattern> {
            return cards.filterIsInstance<NumberCard>()
                .map { Single(it) }.toSet()
        }
    }

}