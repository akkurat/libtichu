package ch.taburett.tichu.cards

interface PlayCard : HandCard {

    /**
     * Effective Value
     * e.g value ch.taburett.tichu.cards.Phoenix simulates
     */
    fun getValue(): Double
    fun asHandcard(): HandCard {
        return this
    }


}