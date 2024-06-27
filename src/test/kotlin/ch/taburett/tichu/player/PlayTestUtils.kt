package ch.taburett.tichu.player

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.common.Player
import ch.taburett.tichu.game.protocol.WrappedPlayerMessage
import ch.taburett.tichu.game.protocol.Message

fun Player.play(card: PlayCard): WrappedPlayerMessage {
    return WrappedPlayerMessage(this, Message.Move(listOf(card)))
}