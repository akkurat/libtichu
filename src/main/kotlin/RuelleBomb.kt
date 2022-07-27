class RuelleBomb(cards: Collection<PlayCard>) : ConcretePattern(TichuPattern.RUELBOMB, cards) {
    companion object : PatternFactory {
        override fun pattern(cards: Collection<PlayCard>): ConcretePattern? {
            if (Ruelle.isValidRuelle(cards)) {
                if (cards.all { it.color() == cards.first().color() }) {
                    return RuelleBomb(cards)
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<ConcretePattern> {
            TODO("Not yet implemented")
        }
    }

}