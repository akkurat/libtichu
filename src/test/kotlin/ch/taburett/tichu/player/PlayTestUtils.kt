package ch.taburett.tichu.player

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.communication.WrappedPlayerMessage
import ch.taburett.tichu.game.communication.Message

fun EPlayer.play(card: PlayCard): WrappedPlayerMessage {
    return WrappedPlayerMessage(this, Message.Move(listOf(card)))
}