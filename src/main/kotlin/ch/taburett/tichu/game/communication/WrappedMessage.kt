package ch.taburett.tichu.game.communication

import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.communication.Message.PlayerMessage
import ch.taburett.tichu.game.communication.Message.ServerMessage

interface WrappedMessage {
    val u: EPlayer
}

data class WrappedServerMessage(override val u: EPlayer, val message: ServerMessage) : WrappedMessage {
    override fun toString(): String = "$u<<$message"
}

data class WrappedPlayerMessage(override val u: EPlayer, val message: PlayerMessage): WrappedMessage {
    override fun toString(): String = "$u>>$message"
}
