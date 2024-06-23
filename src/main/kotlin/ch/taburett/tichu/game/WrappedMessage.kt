package ch.taburett.tichu.game

import ch.taburett.tichu.game.protocol.Message.PlayerMessage
import ch.taburett.tichu.game.protocol.Message.ServerMessage

data class WrappedServerMessage(val u: Player, val message: ServerMessage) {
    override fun toString(): String = "$u<<$message"
}

data class WrappedPlayerMessage(val u: Player, val message: PlayerMessage) {
    override fun toString(): String = "$u>>$message"
}
