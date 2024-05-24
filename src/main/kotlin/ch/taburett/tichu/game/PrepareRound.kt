package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.protocol.*
import kotlin.reflect.KClass

interface State {
    fun complete(): Boolean
    fun reactsTo(value: PlayerMessage): Boolean
    fun react(u: Player, s: PlayerMessage)
}

class PrepareRound(val com: Out) {

    sealed class AckState(val reactsTo: KClass<out Ack>) : State {
        val ack = mutableSetOf<Player>()
        override fun complete(): Boolean {
            return ack.size == playerList.size
        }

        override fun reactsTo(value: PlayerMessage): Boolean {
            return value::class == reactsTo
        }

        override fun react(u: Player, s: PlayerMessage) {
            assert(s::class == reactsTo)
            ack.add(u)
        }
    }

    // enum would actually be fine....
    class bTichu : AckState(Ack.BigTichu::class)
    class preSchupf : AckState(Ack.TichuBeforeSchupf::class)
    class schupfed : AckState(Ack.SchupfcardReceived::class)
    class preGame : AckState(Ack.TichuBeforePlay::class)


    // theory all players could have their own state....
    class SchupfState : State {
        //                           <from, Map<to, handcar>>
        val schupfBuffer: MutableMap<Player, Map<Player, HandCard>> = mutableMapOf()
        override fun complete(): Boolean {
            return schupfBuffer.size == playerList.size
        }

        override fun reactsTo(value: PlayerMessage): Boolean {
            return Schupf::class.isInstance(value)
        }

        override fun react(u: Player, s: PlayerMessage) {
            if (s is Schupf) {
                schupfBuffer[u] = mapSchupfEvent(u, s)
            }
        }
    }

    var isFinished: Boolean = false
    lateinit var currentState: State
    val schupfState = SchupfState()
    val states = listOf(
        bTichu() to {
            cardMap = playerList.zip(first8.chunked(8))
                //                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )
                .associateBy({ z -> z.first }, { z -> z.second.toMutableList() })
            sendStage(Stage.EIGHT_CARDS)
        },
        preSchupf() to {
            for ((k, v) in playerList.zip((last6).chunked(6))) {
                cardMap[k]!!.addAll(v)
            }
            sendStage(Stage.PRE_SCHUPF)
        },
        schupfState to { sendStage(Stage.SCHUPF) },
        schupfed() to { switchCards(schupfState.schupfBuffer) },
        preGame() to { sendStage(Stage.POST_SCHUPF) }
    )
    val stateIterator = states.iterator()

    fun start() {
        val (state, transition) = stateIterator.next()
        currentState = state
        transition()
    }

    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }

    fun sendStage(stage: Stage) {
        for ((u, c) in cardMap) {
            val message = AckGameStage(stage, c)
            sendMessage(WrappedServerMessage(u, message))
        }
    }

    lateinit var cardMap: Map<Player, MutableList<HandCard>>

    fun react(u: Player, s: PlayerMessage) {
        if (currentState.reactsTo(s)) {
            currentState.react(u, s)
            if (currentState.complete()) {
                if (stateIterator.hasNext()) {
                    val (state,transition) = stateIterator.next()
                    currentState = state
                    transition()
                } else {
                    isFinished = true
                }
            }
        } else {
            sendMessage(WrappedServerMessage(u, Rejected("Current State is ${currentState}", s)))
        }
    }

    val first8: List<HandCard>
    val last6: List<HandCard>

    init {
        val (_first8, _last6) = fulldeck.shuffled().chunked(32)
        first8 = _first8
        last6 = _last6
    }

    fun switchCards(
        schupfBuffer: MutableMap<Player, Map<Player, HandCard>>,
    ) {
        val copy = cardMap.toMutableMap()
        // hm... maybe tables?
        val received: Map<Player, MutableMap<Player, HandCard>> =
            cardMap.keys.associateBy({ k -> k }, { mutableMapOf() })

        for ((from, cards) in schupfBuffer) {
            val myCards = copy[from]
            for ((to, card) in cards) {
                myCards!!.remove(card)
                copy[to]!!.add(card)
                received[to]!![from] = card
            }
        }

        for ((u, c) in received) {
            val msg = Schupf(
                c[u.re()]!!,
                c[u.li()]!!,
                c[u.partner()]!!
            )
            sendMessage(WrappedServerMessage(u, msg))
        }
        cardMap = copy

    }

}


fun mapSchupfEvent(u: Player, schupf: Schupf): Map<Player, HandCard> {
    return mapOf(
        u.partner() to schupf.partner,
        u.li() to schupf.li,
        u.re() to schupf.re,
    )
}
