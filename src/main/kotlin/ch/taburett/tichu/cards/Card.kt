
package ch.taburett.tichu.cards

import ch.taburett.tichu.cards.Type.REGULAR


class NumberCard(
    private val value: Int,
    private val color: Color,
    private val points: Int = 0,
    private val name: String,
) : PlayCard {

    constructor(value: Int, color: Color) :
            this(value, color, 0, value.toString())

    constructor(value: Int, color: Color, points: Int) :
            this(value, color, points, value.toString())

    override fun getPoints(): Int {
        return points
    }

    override fun getColor(): Color {
        return color
    }

    override fun getName(): String {
        return "$color $name";
    }

    override fun getSort(): Double {
        return value + color.offset;
    }

    override fun getValue(): Int {
        return value
    }

    override fun toString(): String {
        return color.short + value
    }

    override fun getCode(): String {
        return color.short.lowercase() + codeValue()
    }

    private fun codeValue(): String {
        return when (value) {
            14 -> "A"
            13 -> "K"
            12 -> "Q"
            11 -> "J"
            else -> value.toString()
        }
    }

}

class SpecialCard(
    private val name: String,
    private val short: String,
    private val value: Int,
    private val points: Int = 0,
) : PlayCard {
    override fun getValue(): Int {
        return value;
    }

    override fun getPoints(): Int {
        return points
    }

    override fun getColor(): Color {
        return Color.SPECIAL
    }

    override fun getName(): String {
        return name
    }

    override fun getSort(): Double {
        return value.toDouble();
    }

    override fun getCode(): String {
        return short.lowercase();
    }

    override fun toString(): String {
        return short
    }

}

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

interface PlayCard : HandCard {

    /**
     * Effective Value
     * e.g value ch.taburett.tichu.cards.Phoenix simulates
     */
    fun getValue(): Int
    fun asHandcard(): HandCard {
        return this
    }


}

enum class Type {
    REGULAR,
    SPECIAL
}

/**
 * Red Blue Green Key (black)
 */
enum class Color(val offset: Double, val type: Type, val short: String, val color: String, val colorShort: String) {
    JADE(0.1, REGULAR, "J", "Green", "G"),
    SWORDS(0.2, REGULAR, "D", "Black", "K"),
    PAGODAS(0.3, REGULAR, "P", "Blue", "B"),
    STARS(0.4, REGULAR, "S", "Green", "R"),
    SPECIAL(0.0, Type.SPECIAL, "X", "X", "X")
}

class Phoenix : HandCard {
    override fun getPoints(): Int {
        return -25;
    }

    override fun getColor(): Color {
        return Color.SPECIAL
    }

    override fun getName(): String {
        return "Phoenix"
    }

    override fun getSort(): Double {
        return 15.0
    }

    fun asPlayCard(value: Int): PhoenixPlaycard {
        if (MAH.getValue() <= value && value <= 15)
            return PhoenixPlaycard(this, value)
        throw IllegalArgumentException("Naughty boy")
    }


    override fun toString(): String {
        return "PHX"
    }

    override fun getCode(): String {
        return "phx"
    }

}

class PhoenixPlaycard(private val phoenix: Phoenix, private val value: Int) : HandCard by phoenix, PlayCard {
    override fun getValue(): Int {
        return value
    }

    override fun toString(): String {
        return "PHX$value"
    }

    override fun asHandcard(): HandCard {
        return phoenix;
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhoenixPlaycard) return false

        if (phoenix != other.phoenix) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = phoenix.hashCode()
        result = 31 * result + value
        return result
    }

}