package ch.taburett.tichu.cards

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

    fun asPlayCard(value: Number): PhoenixPlaycard {
        if (MAH.getValue() <= value.toDouble() && value.toDouble() <= 14.5)
            return PhoenixPlaycard(this, value.toDouble())
        throw IllegalArgumentException("Naughty boy: $value is not valid as value")
    }


    override fun toString(): String {
        return "PHX"
    }

    override fun getCode(): String {
        return "phx"
    }

}