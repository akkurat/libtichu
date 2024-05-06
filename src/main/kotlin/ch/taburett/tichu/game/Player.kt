package ch.taburett.tichu.game

enum class Player(val value: String, val group: String) {
    A1("A1", "A"),
    B1("B1", "B"),
    A2("A2", "A"),
    B2("B2", "B");
    // in theory also this could be an enum...
    fun partner(): Player {
        return Player.entries[this.ordinal+2%4]
    }
    fun re(): Player {
        return Player.entries[this.ordinal-1%4]
    }
    fun li(): Player {
        return Player.entries[this.ordinal+1%4]
    }
}


