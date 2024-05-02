package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.cards.fulldeck
import ru.nsk.kstatemachine.*

sealed class States : DefaultState() {
    object bTichu : States()
    object preSchupf : States()
    object schupf : States()
    object postSchupf : States(), FinalState


}

object SwitchEvent : Event
object AckEvent : Event

class MutableRound//                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )

    (start: Boolean = true) {

    var machine: StateMachine

    val tricks: List<Trick> = ArrayList()

    enum class Player(val value: String, val group: String) {
        A1("A1", "A"),
        B1("B1", "B"),
        A2("A2", "A"),
        B2("B2", "B")
    }

    val players = Player.entries.toList()

    val ackG8 = mutableListOf<Player>()
    val ack14 = mutableListOf<Player>()
    val schupfed = mutableMapOf<Player, Map<Player, HandCard>>()

    var cardMap: Map<Player, MutableList<HandCard>> = mutableMapOf()

    val playedCars = mutableListOf<PlayCard>()

    init {
        val (first8, last6) = fulldeck.chunked(32)
        machine = createStdLibStateMachine("TStateRound", start = start) {
            addInitialState(States.bTichu) {
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
                transition<AckEvent> {
                    targetState = States.preSchupf
                    guard = { ackG8.containsAll(players) }
                }
            }

            addState(States.preSchupf) {
                transition<AckEvent> {
                    onTriggered { println("preSchupf Trans") }
                    targetState = States.schupf
                    guard = { ack14.containsAll(players) }
                }
            }

            addState(States.schupf) {
                transitionOn<SchupfEvent> {
                    onTriggered { se ->
                        onTriggered { println("schupfed") }
                    }
                    targetState = { if (schupfed.keys.containsAll(players)) States.postSchupf else States.schupf }
                }

            }
            addFinalState(States.postSchupf) {
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

class SchupfEvent(val user: MutableRound.Player, val cards: Map<MutableRound.Player, HandCard>) : Event {
    init {
        if (cards.containsKey(user)) {
            throw IllegalArgumentException("")
        }
        if (cards.size != MutableRound.Player.entries.size - 1) {
            throw IllegalArgumentException("")
        }
    }
}


