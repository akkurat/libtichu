package ch.taburett.tichu.cards

class Single(val card: PlayCard) : TichuPattern(TichuPatternType.SINGLDE, setOf(card)) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.size == 1) {
                return Single(cards.first())
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            return cards.filterIsInstance<NumberCard>()
                .map { Single(it) }.toSet()
        }
    }

}