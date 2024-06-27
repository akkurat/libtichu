
package ch.taburett.tichu.cards


data class PhoenixPlaycard(private val phoenix: Phoenix, private val value: Double) : HandCard by phoenix, PlayCard {
    override fun getValue(): Double {
        return value
    }

    override fun toString(): String {
        return "PHX$value"
    }

    override fun asHandcard(): HandCard {
        return phoenix;
    }

    override fun getSort(): Double {
        return value
    }
}