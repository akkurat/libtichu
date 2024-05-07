@file:JsExport
package ch.taburett.tichu.cards

import ch.taburett.tichu.cards.Type.REGULAR
import kotlin.js.JsExport
import kotlin.js.JsName


class NumberCard(private val value: Int, private val color: Color, private val points: Int = 0, private val name: String) : PlayCard {

    @JsName("simple")
    constructor(value: Int, color: Color) :
            this(value, color, 0, value.toString())

    @JsName("withPoints")
    constructor(value: Int, color: Color, points: Int) :
            this(value, color, points, value.toString())

    override fun points(): Int {
        return points
    }

    override fun color(): Color {
        return color
    }

    override fun name(): String {
        return "$color $name";
    }

    override fun sort(): Double {
        return value + color.offset;
    }

    override fun value(): Int {
        return value
    }

    override fun toString(): String {
        return color.short + value
    }

}

class SpecialCard(private val name: String, private val short: String, private val value: Int, private val points: Int = 0) : PlayCard {
    override fun value(): Int {
        return value;
    }

    override fun points(): Int {
        return points
    }

    override fun color(): Color {
        return Color.SPECIAL
    }

    override fun name(): String {
        return name
    }

    override fun sort(): Double {
        return value.toDouble();
    }

    override fun toString(): String {
        return short
    }

}

interface HandCard : Comparable<HandCard> {
    fun points(): Int
    fun color(): Color
    fun name(): String
    fun sort(): Double
    override fun compareTo(other: HandCard): Int {
        return this.sort().compareTo(other.sort())
    }
}

interface PlayCard : HandCard {

    /**
     * Effective Value
     * e.g value ch.taburett.tichu.cards.Phoenix simulates
     */
    fun value(): Int
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
    override fun points(): Int {
        return -25;
    }

    override fun color(): Color {
        return Color.SPECIAL
    }

    override fun name(): String {
        return "Phoenix"
    }

    override fun sort(): Double {
        return 15.0
    }

    fun asPlayCard(value: Int): PhoenixPlaycard {
        if (MAJ.value() < value && value <= J14.value())
            return PhoenixPlaycard(this, value)
        throw IllegalArgumentException("Naughty boy")
    }


    override fun toString(): String {
        return "PHX"
    }

}

class PhoenixPlaycard(private val phoenix: Phoenix, private val value: Int) : HandCard by phoenix, PlayCard {
    override fun value(): Int {
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