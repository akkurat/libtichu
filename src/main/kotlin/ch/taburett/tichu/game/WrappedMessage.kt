package ch.taburett.tichu.game

data class WrappedServerMessage(val u: Player, val message: ServerMessage)

data class WrappedPlayerMessage(val u: Player, val message: PlayerMessage)
