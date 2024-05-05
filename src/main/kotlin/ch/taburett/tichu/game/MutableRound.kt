package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ru.nsk.kstatemachine.*


//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}


class MutableRound//                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )

    (start: Boolean = true) {

    sealed class AckState : DefaultState() {
        val ack = mutableSetOf<Player>()
    }

    object bTichu : AckState()
    object preSchupf : AckState()
    object postSchupf : AckState()
    object AckEvent : Event

    object schupf : DefaultState() {
        val schupfBuffer: MutableMap<Player, Map<Player, HandCard>> = mutableMapOf()
    }

    class PlayerState(val player: Player) : DefaultState()
    class RegularMoveEvent : Event
    class BombMoveEvent(val player: Player) : Event

    val playerStates: Map<Player, PlayerState> = Player.entries.associateBy({ it }) { PlayerState(it) }

    var machine: StateMachine

    val players = Player.entries.toList()

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
                        cardMap[k]!!.addAll(v)
                    }
                    println("14 cards")
                }
                transitionOn<AckEvent> {
                    targetState = { preSchupf }
                    guard = { this@addInitialState.ack.containsAll(players) }
                }
            }


            addState(preSchupf) {
                transition<AckEvent> {
                    onTriggered { println("preSchupf Trans") }
                    targetState = schupf
                    guard = { this@addState.ack.containsAll(players) }
                }
            }

            val selectStartPlayer = choiceState {
                println(this)

                val key = cardMap
                    .filter { it.value.contains(MAJ) }
                    .map { it.key }
                    .first()

                playerStates[key]!!
            }

            playerStates.values.map { addState(it) }
                .forEach {
                    transition<RegularMoveEvent> {
                        targetState = playerStates[nextPlayer(it)]
                    }
                    transitionOn<BombMoveEvent> {
                        // todo: go to player who played the bomb

                        targetState = { playerStates[event.player]!! }
                    }
                }

            addState(schupf) {
                onEntry { println("hello") }
                transition<SchupfEvent> {
                    onTriggered { this@addState.schupfBuffer.put(it.event.user, it.event.cards) }
                    targetState = postSchupf
                    guard = { this@addState.schupfBuffer.size == 4 }
//                    targetState = { if (schupf.players.size == 4) postSchupf else schupf }
                }
                onExit { switchCards(schupfBuffer) }
            }

            addState(postSchupf) {
                onEntry {
                    println("postSchupf")
                }
                transition<AckEvent> {
                    targetState = selectStartPlayer
                    guard = { this@addState.ack.size == 4 }
                }
            }

            onFinished { println("finished") }
        }
    }

    private fun nextPlayer(it: PlayerState): Player {
        val nextIdx = ((players.indexOf(it.player)) + 1) % players.size
        return players[nextIdx]
    }

    private fun switchCards(schupfBuffer: MutableMap<Player, Map<Player, HandCard>>) {
        // remove cards
        val copy = cardMap.toMutableMap()

        for (e in schupfBuffer) {
            val myCards = copy[e.key]
            for ((u, c) in e.value) {
                myCards!!.remove(c)
                copy[u]!!.add(c)
            }
        }
        cardMap = copy
    }

    fun ack(u: Player, s: String) {
        when (s) {
            // todo: more consistent naming
            "G8" -> bTichu.ack.add(u)
            "14" -> preSchupf.ack.add(u)
            "PostSchupf" -> postSchupf.ack.add(u)
        }
        machine.processEventBlocking(AckEvent)
    }

    fun schupf(payload: SchupfEvent) {
        schupf.schupfBuffer[payload.user] = payload.cards
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


