package ch.taburett.tichu.game.core.preparation

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.game.communication.Message
import ch.taburett.tichu.game.communication.WrappedServerMessage
import ch.taburett.tichu.game.core.common.PlayerETichuMutableMap
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.ETichu
import ch.taburett.tichu.game.core.common.playerList
import kotlin.reflect.KClass

sealed class AckState(val name: String, private vararg val reactsTo: KClass<out Message.PlayerMessage>) :
    IPreparationState {

    private val ack = mutableSetOf<EPlayer>()

    override fun complete(): Boolean {
        return ack.size == playerList.size
    }

    override fun reactsTo(value: Message.PlayerMessage): Boolean {
        return reactsTo.contains(value::class)

    }

    override fun react(
        u: EPlayer,
        s: Message.PlayerMessage,
        cardMap: Map<EPlayer, MutableList<HandCard>>,
        tichuMap: PlayerETichuMutableMap,
        name: String?,
    ): WrappedServerMessage? {
        assert(reactsTo(s))
        if( s is Message.Announce.BigTichu) {
           println("$name $u big tichu")
            // todo gamelog
            tichuMap[u] = ETichu.BIG
        } else if ( s is Message.Announce.SmallTichu) {
            println("$name $u small tichu ${this.name}")
            // todo gamelog
            tichuMap[u] = ETichu.SMALL
        }
        ack.add(u)
        return null
    }
}