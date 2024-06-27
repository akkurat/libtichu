package ch.taburett.tichu.cards

/**
 * Red Blue Green Key (black)
 */
enum class Color(val offset: Double, val type: Type, val short: String, val color: String, val colorShort: String) {
    JADE(0.1, Type.REGULAR, "J", "Green", "G"),
    SWORDS(0.2, Type.REGULAR, "D", "Black", "K"),
    PAGODAS(0.3, Type.REGULAR, "P", "Blue", "B"),
    STARS(0.4, Type.REGULAR, "S", "Green", "R"),
    SPECIAL(0.0, Type.SPECIAL, "X", "X", "X")
}