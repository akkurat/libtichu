package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.cards.fulldeck
import ru.nsk.kstatemachine.*
import java.util.EnumMap



//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}
object AckEvent: Event

class MutableRound//                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )

    (start: Boolean = true) {

    object bTichu : DefaultState()

    lateinit var preSchupf : State
    object schupf: DefaultState () {
        val players: MutableMap<Player,Map<Player,HandCard>> = mutableMapOf()
    }


    lateinit var postSchupf: DataState<Map<Player, HandCard>>

    var machine: StateMachine

    val players = Player.entries.toList()

    val ackG8 = mutableListOf<Player>()
    val ack14 = mutableListOf<Player>()
    val schupfed = mutableMapOf<Player, Map<Player, HandCard>>()

    var cardMap: Map<Player, MutableList<HandCard>> = mutableMapOf()

    val playedCars = mutableListOf<PlayCard>()

    init {
        val (first8, last6) = fulldeck.chunked(32)
        machine = createStdLibStateMachine(
            "TStateRound", start = start
        ) {
            addInitialState(bTichu) {
                onEntry {
                    cardMap = players.zip(first8.chunked(8))
//                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )
                        .associateBy({ z -> z.first }, { z -> z.second.toMutableList() })

                    println("8 Cards")
                }
                onExit {
                    for ((k, v) in players.zip((last6).chunked(6))) {
                        cardMap.get(k)!!.addAll(v)
                    }
                    println("14 cards")
                }
                transitionOn<AckEvent> {

                    targetState = {preSchupf}
                    guard = { ackG8.containsAll(players) }
                }
            }


            preSchupf = state() {
                transition<AckEvent>  {
                    onTriggered { println("preSchupf Trans") }
                    targetState = schupf
                    guard = { ack14.containsAll(players) }
                }
            }

            addState( schupf ) {

                dataTransitionOn<SchupfEvent, Map<Player, HandCard>> {
                    onTriggered { schupf.players.put(it.event.user, it.event.cards) }
                    targetState = {postSchupf}
                    guard = { schupf.players.size == 4}
//                    targetState = { if (schupf.players.size == 4) postSchupf else schupf }
                }
            }

            postSchupf = finalDataState {
                onEntry {
                    println("postSchupf")
                }
            }
        }
    }

    fun ack(u: Player, s: String) {
        when (s) {
            "G8" -> ackG8.add(u)
            "14" -> ack14.add(u)
        }
        machine.processEventBlocking(AckEvent)
    }

    fun schupf(payload: SchupfEvent) {
        schupfed[payload.user] = payload.cards
        machine.processEventBlocking(payload)
    }
}

class SchupfEvent(val user: Player, val cards: Map<Player, HandCard>) : DataEvent<Map<Player, HandCard>> {
    init {
        if (cards.containsKey(user)) {
            throw IllegalArgumentException("")
        }
        if (cards.size != Player.entries.size - 1) {
            throw IllegalArgumentException("")
        }
    }

    override val data: Map<Player, HandCard>
        get() = cards
}


