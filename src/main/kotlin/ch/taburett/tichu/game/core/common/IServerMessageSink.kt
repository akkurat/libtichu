package ch.taburett.tichu.game.core.common

import ch.taburett.tichu.game.communication.WrappedServerMessage

fun interface IServerMessageSink {
    fun send(wrappedServerMessage: WrappedServerMessage)
}