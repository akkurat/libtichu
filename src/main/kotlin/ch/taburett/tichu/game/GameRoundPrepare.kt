package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.gamelog.IPlayLogEntry
import ch.taburett.tichu.game.protocol.Message.*
import kotlin.reflect.KClass

interface State {
    fun complete(): Boolean
    fun reactsTo(value: PlayerMessage): Boolean
    fun react(
        u: Player,
        s: PlayerMessage,
        cardMap: Map<Player, MutableList<HandCard>>,
        tichuMap: PlayerETichuMutableMap,
        name: String?
    ): WrappedServerMessage?
}


class PrepareRound(val com: Out, override val name: String? = null) : TichuGameStage {

    private lateinit var cards6: Map<Player, List<HandCard>>
    private lateinit var cards8: Map<Player, MutableList<HandCard>>
    private lateinit var schupfInfo: PreparationInfo.SchupfLog

    private val tichuMap: PlayerETichuMutableMap = Player.entries
        .associateWith { ETichu.NONE }.toMutableMap()

    sealed class AckState(val name: String, private vararg val reactsTo: KClass<out PlayerMessage>) : State {

        private val ack = mutableSetOf<Player>()

        override fun complete(): Boolean {
            return ack.size == playerList.size
        }

        override fun reactsTo(value: PlayerMessage): Boolean {
            return reactsTo.contains(value::class)

        }

        override fun react(
            u: Player,
            s: PlayerMessage,
            cardMap: Map<Player, MutableList<HandCard>>,
            tichuMap: PlayerETichuMutableMap,
            name: String?,
        ): WrappedServerMessage? {
            assert(reactsTo(s))
            if( s is Announce.BigTichu ) {
               println("$name $u big tichu")
                // todo gamelog
                tichuMap[u] = ETichu.BIG
            } else if ( s is Announce.SmallTichu ) {
                println("$name $u small tichu ${this.name}")
                // todo gamelog
                tichuMap[u] = ETichu.SMALL
            }
            ack.add(u)
            return null
        }
    }

    // enum would actually be fine....
    class bTichu : AckState("BigTichu", Ack.BigTichu::class, Announce.BigTichu::class)
    class preSchupf : AckState("PreSchupf", Ack.TichuBeforeSchupf::class, Announce.SmallTichu::class)
    class schupfed : AckState("AfterSchupf", Ack.SchupfcardReceived::class, Announce.SmallTichu::class)
    class preGame : AckState("PreGame", Ack.TichuBeforePlay::class, Announce.SmallTichu::class)

    class SchupfState : State {
        //                           <from, Map<to, handcar>>
        val schupfBuffer: MutableMap<Player, Map<Player, HandCard>> = mutableMapOf()
        override fun complete(): Boolean {
            return schupfBuffer.size == playerList.size
        }

        override fun reactsTo(value: PlayerMessage): Boolean {
            return Schupf::class.isInstance(value)
        }

        override fun react(
            u: Player,
            s: PlayerMessage,
            cardMap: Map<Player, MutableList<HandCard>>,
            tichuMap: PlayerETichuMutableMap,
            name: String?,
        ): WrappedServerMessage? {
            if (s is Schupf) {
                if (checkCardsLegal(u, s, cardMap.getValue(u))) {
                    schupfBuffer[u] = mapSchupfEvent(u, s)
                } else {
                    return WrappedServerMessage(u, Rejected("Cheating!"))
                }
            }
            return null
        }

        private fun checkCardsLegal(u: Player, s: Schupf, value: List<HandCard>): Boolean {
            val avCards = value.toMutableList()
            return avCards.remove(s.re) && avCards.remove(s.li) && avCards.remove(s.partner)
        }
    }

    var isFinished: Boolean = false
    lateinit var currentState: State
    val schupfState = SchupfState()
    val states = listOf(
        bTichu() to {
            cardMap = playerList.zip(first8.chunked(8))
                .associateBy({ z -> z.first }, { z -> z.second.toMutableList() })
            cards8 = cardMap.toMap()
            sendStage(Stage.EIGHT_CARDS)
        },
        preSchupf() to {
            val zipped = playerList.zip((last6).chunked(6))
            for ((k, v) in zipped) {
                cardMap[k]!!.addAll(v)
            }
            cards6 = zipped.toMap()
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

    private fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }

    private fun sendStage(stage: Stage) {
        for ((u, c) in cardMap) {
            val message = AckGameStage(u,stage, c, tichuMap)
            sendMessage(WrappedServerMessage(u, message))
        }
    }

    lateinit var cardMap: Map<Player, MutableList<HandCard>>

    fun react(u: Player, s: PlayerMessage) {
        if (currentState.reactsTo(s)) {
            val error = currentState.react(u, s, cardMap, tichuMap, name)
            if (error != null) {
                sendMessage(error)
            } else if (currentState.complete()) {
                if (stateIterator.hasNext()) {
                    val (state, transition) = stateIterator.next()
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
                c[u.re]!!,
                c[u.li]!!,
                c[u.partner]!!
            )
            sendMessage(WrappedServerMessage(u, msg))
        }

        schupfInfo = PreparationInfo.SchupfLog(to = schupfBuffer, from = received)
        cardMap = copy

    }

    fun receivePlayerMessage(wrappedPlayerMessage: WrappedPlayerMessage) {

        val u = wrappedPlayerMessage.u
        when (val m = wrappedPlayerMessage.message) {
            is Ack, is Schupf, is Announce.SmallTichu, is Announce.BigTichu -> react(u, m)
            else -> {
                sendMessage(WrappedServerMessage(u, Rejected("Prepare round can't handle this message", m)))
            }
        }
    }

    val preparationInfo: PreparationInfo
        get() {
            return PreparationInfo(cards8, cards6, schupfInfo, listOf(), tichuMap.toMap())
        }

}

data class PreparationInfo(
    val cards8: Map<Player, List<HandCard>>,
    val cards6: Map<Player, List<HandCard>>,
    val schupf: SchupfLog,
    val tichuLog: List<IPlayLogEntry>,
    val tichuMap: Map<Player, ETichu>,
) {
    data class SchupfLog(
        val to: Map<Player, Map<Player, HandCard>>,
        val from: Map<Player, Map<Player, HandCard>>,
    )
}

fun mapSchupfEvent(u: Player, schupf: Schupf): Map<Player, HandCard> {
    return mapOf(
        u.partner to schupf.partner,
        u.li to schupf.li,
        u.re to schupf.re,
    )
}
