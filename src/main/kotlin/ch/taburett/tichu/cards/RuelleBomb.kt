package ch.taburett.tichu.cards

class RuelleBomb(cards: Collection<PlayCard>) : TichuPattern(TichuPatternType.RUELBOMB, cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (Ruelle.isValidRuelle(cards)) {
                if (cards.all { it.color() == cards.first().color() }) {
                    return RuelleBomb(cards)
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            TODO("Not yet implemented")
        }
    }

}