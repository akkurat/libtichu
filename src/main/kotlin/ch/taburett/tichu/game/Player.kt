package ch.taburett.tichu.game

import ch.taburett.tichu.game.PlayerGroup.*

enum class PlayerGroup {

    A {
        override fun other(): PlayerGroup = B
    },
    B {
        override fun other(): PlayerGroup = A
    };

    abstract fun other(): PlayerGroup
}

enum class Player(val value: String, val playerGroup: PlayerGroup) {
    A1("A1", A),
    B1("B1", B),
    A2("A2", A),
    B2("B2", B);

    val li: Player
        get() {
            return Player.entries[(this.ordinal + 3) % 4]
        }

    val partner: Player
        get() {
            return Player.entries[(this.ordinal + 2) % 4]
        }

    val re: Player
        get() {
            return Player.entries[(this.ordinal + 1) % 4]
        }
}

val playerList = Player.entries.toList()




