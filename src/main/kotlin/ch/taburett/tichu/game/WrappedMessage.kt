package ch.taburett.tichu.game

import ch.taburett.tichu.game.protocol.PlayerMessage
import ch.taburett.tichu.game.protocol.ServerMessage

data class WrappedServerMessage(val u: Player, val message: ServerMessage)

data class WrappedPlayerMessage(val u: Player, val message: PlayerMessage)
