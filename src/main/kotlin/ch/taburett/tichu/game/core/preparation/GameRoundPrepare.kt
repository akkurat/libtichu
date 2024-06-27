package ch.taburett.tichu.game.core.preparation

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.core.*
import ch.taburett.tichu.game.communication.Message.*
import ch.taburett.tichu.game.communication.WrappedPlayerMessage
import ch.taburett.tichu.game.communication.WrappedServerMessage
import ch.taburett.tichu.game.core.common.*


class GameRoundPrepare(val com: IServerMessageSink, override val name: String? = null) : ITichuGameStage {

    private lateinit var cards6: Map<EPlayer, List<HandCard>>
    private lateinit var cards8: Map<EPlayer, MutableList<HandCard>>
    private lateinit var schupfInfo: PreparationInfo.SchupfLog

    private val tichuMap: PlayerETichuMutableMap = EPlayer.entries
        .associateWith { ETichu.NONE }.toMutableMap()

    // enum would actually be fine....
    class bTichu : AckState("BigTichu", Ack.BigTichu::class, Announce.BigTichu::class)
    class preSchupf : AckState("PreSchupf", Ack.TichuBeforeSchupf::class, Announce.SmallTichu::class)
    class schupfed : AckState("AfterSchupf", Ack.SchupfcardReceived::class, Announce.SmallTichu::class)
    class preGame : AckState("PreGame", Ack.TichuBeforePlay::class, Announce.SmallTichu::class)

    var isFinished: Boolean = false
    lateinit var currentPreparationState: IPreparationState
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
        currentPreparationState = state
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

    lateinit var cardMap: Map<EPlayer, MutableList<HandCard>>

    fun react(u: EPlayer, s: PlayerMessage) {
        if (currentPreparationState.reactsTo(s)) {
            val error = currentPreparationState.react(u, s, cardMap, tichuMap, name)
            if (error != null) {
                sendMessage(error)
            } else if (currentPreparationState.complete()) {
                if (stateIterator.hasNext()) {
                    val (state, transition) = stateIterator.next()
                    currentPreparationState = state
                    transition()
                } else {
                    isFinished = true
                }
            }
        } else {
            sendMessage(WrappedServerMessage(u, Rejected("Current State is ${currentPreparationState}", s)))
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
        schupfBuffer: MutableMap<EPlayer, Map<EPlayer, HandCard>>,
    ) {
        val copy = cardMap.toMutableMap()
        // hm... maybe tables?
        val received: Map<EPlayer, MutableMap<EPlayer, HandCard>> =
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

