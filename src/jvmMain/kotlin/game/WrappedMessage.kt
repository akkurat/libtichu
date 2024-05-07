package game

data class WrappedServerMessage(val u: Player, val message: ServerMessage)

data class WrappedUserMessage(val u: Player, val message: PlayerMessage)
