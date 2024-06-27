package ch.taburett.tichu.game.core.preparation

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.game.communication.Message
import ch.taburett.tichu.game.communication.WrappedServerMessage
import ch.taburett.tichu.game.core.PlayerETichuMutableMap
import ch.taburett.tichu.game.core.common.EPlayer

interface IPreparationState {
    fun complete(): Boolean
    fun reactsTo(value: Message.PlayerMessage): Boolean
    fun react(
        u: EPlayer,
        s: Message.PlayerMessage,
        cardMap: Map<EPlayer, MutableList<HandCard>>,
        tichuMap: PlayerETichuMutableMap,
        name: String?
    ): WrappedServerMessage?
}