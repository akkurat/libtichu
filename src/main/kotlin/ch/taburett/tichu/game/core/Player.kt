package ch.taburett.tichu.game.core

import ch.taburett.tichu.game.core.Player.*
import ch.taburett.tichu.game.core.PlayerGroup.*

enum class PlayerGroup(vararg _players: Player) {

    A {
        // inherent circular dependency hence must be function or at least lazily evaluated/**/
        override val players: List<Player> by lazy { listOf(A1, A2) }

        override fun other(): PlayerGroup = B
    },
    B {
        override val players: List<Player> by lazy { listOf(B1, B2) }

        override fun other(): PlayerGroup = A
    };

    abstract val players: List<Player>
    abstract fun other(): PlayerGroup
}

enum class Player(val value: String, val playerGroup: PlayerGroup) {
    A1("A1", A),
    B1("B1", B),
    A2("A2", A),
    B2("B2", B);

    val next: Player
        get() {
            return Player.entries[(this.ordinal + 3) % 4]
        }

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




