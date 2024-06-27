package ch.taburett.tichu.cards

interface HandCard : Comparable<HandCard> {
    fun getPoints(): Int
    fun getColor(): Color
    fun getName(): String
    fun getSort(): Double
    fun getCode(): String
    override fun compareTo(other: HandCard): Int {
        return this.getSort().compareTo(other.getSort())
    }
}