package ch.taburett.tichu.game.protocol

import ch.taburett.tichu.game.core.common.Player
import ch.taburett.tichu.game.protocol.Message.PlayerMessage
import ch.taburett.tichu.game.protocol.Message.ServerMessage

interface WrappedMessage {
    val u: Player
}

data class WrappedServerMessage(override val u: Player, val message: ServerMessage) : WrappedMessage {
    override fun toString(): String = "$u<<$message"
}

data class WrappedPlayerMessage(override val u: Player, val message: PlayerMessage): WrappedMessage {
    override fun toString(): String = "$u>>$message"
}
