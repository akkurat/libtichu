package ch.taburett.tichu.game.core.preparation

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.game.communication.Message
import ch.taburett.tichu.game.communication.WrappedServerMessage
import ch.taburett.tichu.game.core.common.PlayerETichuMutableMap
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.playerList

class SchupfState : IPreparationState {
    //                           <from, Map<to, handcar>>
    val schupfBuffer: MutableMap<EPlayer, Map<EPlayer, HandCard>> = mutableMapOf()
    override fun complete(): Boolean {
        return schupfBuffer.size == playerList.size
    }

    override fun reactsTo(value: Message.PlayerMessage): Boolean {
        return Message.Schupf::class.isInstance(value)
    }

    override fun react(
        u: EPlayer,
        s: Message.PlayerMessage,
        cardMap: Map<EPlayer, MutableList<HandCard>>,
        tichuMap: PlayerETichuMutableMap,
        name: String?,
    ): WrappedServerMessage? {
        if (s is Message.Schupf) {
            if (Companion.checkCardsLegal(u, s, cardMap.getValue(u))) {
                schupfBuffer[u] = Companion.mapSchupfEvent(u, s)
            } else {
                return WrappedServerMessage(u, Message.Rejected("Cheating!"))
            }
        }
        return null
    }

    companion object {
        private fun mapSchupfEvent(u: EPlayer, schupf: Message.Schupf): Map<EPlayer, HandCard> {
            return mapOf(
                u.partner to schupf.partner,
                u.li to schupf.li,
                u.re to schupf.re,
            )
        }

        private fun checkCardsLegal(u: EPlayer, s: Message.Schupf, value: List<HandCard>): Boolean {
            val avCards = value.toMutableList()
            return avCards.remove(s.re) && avCards.remove(s.li) && avCards.remove(s.partner)
        }
    }
}