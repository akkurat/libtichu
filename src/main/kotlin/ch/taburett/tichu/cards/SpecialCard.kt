package ch.taburett.tichu.cards

class SpecialCard(
    private val name: String,
    private val short: String,
    private val value: Int,
    private val points: Int = 0,
) : PlayCard {
    override fun getValue(): Double {
        return value.toDouble();
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