package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.MutableRound.*
import ru.nsk.kstatemachine.*

sealed class AckState : DefaultState() {
    val ack = mutableSetOf<Player>()
}


class PrepareRound(val com: Out) {

    object bTichu : AckState()
    object preSchupf : AckState()
    object schupfed : AckState()
    object preGame : AckState()
    // theory all players could have their own state....
    object schupf : DefaultState() {
        val schupfBuffer: MutableMap<Player, Map<Player, HandCard>> = mutableMapOf()
    }

    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }

    fun sendStage(stage: Stage, cardMap: Map<Player, MutableList<HandCard>>) {
        for ((u, c) in cardMap) {
            val message = AckGameStage(stage, c)
            sendMessage(WrappedServerMessage(u, message))
        }
    }

    lateinit var cardMap: Map<Player, MutableList<HandCard>>
    fun ack(u: Player, s: Ack) {
        when (s) {
            // todo: more consistent naming
            is Ack.BigTichu -> bTichu.ack.add(u)
            is Ack.TichuBeforeSchupf -> preSchupf.ack.add(u)
            is Ack.SchupfcardReceived -> schupfed.ack.add(u)
            is Ack.TichuBeforePlay -> preGame.ack.add(u)
        }
        machine.processEventBlocking(AckEvent)
    }

    val first8: List<HandCard>
    val last6: List<HandCard>

    init {
        val (_first8, _last6) = fulldeck.shuffled().chunked(32)
        first8 = _first8
        last6 = _last6
    }

    val machine = createStdLibStateMachine("PrepareRound", start = false) {

        addInitialState(bTichu) {
            onEntry {
                cardMap = playerList.zip(first8.chunked(8))
                    //                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )
                    .associateBy({ z -> z.first }, { z -> z.second.toMutableList() })
                sendStage(Stage.EIGHT_CARDS, cardMap)
            }

            transitionOn<AckEvent> {
                targetState = { preSchupf }
                guard = { this@addInitialState.ack.containsAll(playerList) }
            }

//                transitionOn<TichuEvent> {
//                    onTriggered {  }
//                    targetState = { preSchupf }
//                }

        }

        addState(preSchupf) {
            onEntry {
                for ((k, v) in playerList.zip((last6).chunked(6))) {
                    cardMap[k]!!.addAll(v)
                }
                sendStage(Stage.PRE_SCHUPF, cardMap)
            }
            transition<AckEvent> {
                onTriggered { println("preSchupf Trans") }
                targetState = schupf
                guard = { this@addState.ack.containsAll(playerList) }
            }
        }
        addState(schupf) {
            onEntry {
                println("hello")
                sendStage(Stage.SCHUPF, cardMap)
            }
            transition<SchupfEvent> {
                onTriggered { this@addState.schupfBuffer.put(it.event.user, it.event.cards) }
                targetState = schupfed
                guard = { this@addState.schupfBuffer.size == 4 }
//                    targetState = { if (schupf.playerList.size == 4) postSchupf else schupf }
            }
            // sends schupfed cards
            onExit { switchCards(this@PrepareRound, schupfBuffer, cardMap) }
        }

        addState(schupfed) {
            transition<AckEvent> {
                targetState = preGame
                guard = { this@addState.ack.size == 4 }
            }
        }

        val end = finalState { }
        addState(preGame) {
            onEntry {
                sendStage(Stage.POST_SCHUPF, cardMap)
            }
            transition<AckEvent> {
                targetState = end
                guard = { this@addState.ack.size == 4 }
            }
        }
    }

    fun schupf(payload: SchupfEvent) {
        schupf.schupfBuffer[payload.user] = payload.cards
        machine.processEventBlocking(payload)
    }

}


fun switchCards(
    prepareRound: PrepareRound, schupfBuffer: MutableMap<Player, Map<Player, HandCard>>,
    cardMap: Map<Player, MutableList<HandCard>>,
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
        prepareRound.sendMessage(WrappedServerMessage(u, msg))
    }

}