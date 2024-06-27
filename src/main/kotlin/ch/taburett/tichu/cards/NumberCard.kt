package ch.taburett.tichu.cards

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

    override fun getValue(): Double {
        return value.toDouble()
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