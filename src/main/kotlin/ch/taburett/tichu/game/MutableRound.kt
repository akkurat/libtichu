package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.MAH
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.patterns.LegalType
import ru.nsk.kstatemachine.*


//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}


class MutableRound(val com: Out, start: Boolean = true) {

    sealed class AckState : DefaultState() {
        val ack = mutableSetOf<Player>()
    }

    object bTichu : AckState()
    object preSchupf : AckState()
    object schupfed : AckState()
    object postSchupf : AckState()
    object AckEvent : Event

    object schupf : DefaultState() {
        val schupfBuffer: MutableMap<Player, Map<Player, HandCard>> = mutableMapOf()
    }

    class PlayerState(val player: Player) : DefaultState()

    class RegularMoveEvent(val player: Player, val cards: Collection<PlayCard>) : Event
    class DogMoveEvent(val player: Player) : Event
    class BombMoveEvent(val player: Player) : Event
    class TichuEvent(player: Player) : Event

    val playerStates: Map<Player, PlayerState> = Player.entries.associateBy({ it }) { PlayerState(it) }

    var machine: StateMachine

    val players = Player.entries.toList()

    var cardMap: Map<Player, MutableList<HandCard>> = mutableMapOf()

    var table = ArrayList<Played>()

    var tricks = ArrayList<List<Played>>()


    init {
        val (first8, last6) = fulldeck.shuffled().chunked(32)
        machine = createStdLibStateMachine(
            "TStateRound", start = start
        ) {
            addInitialState(bTichu) {
                onEntry {
                    cardMap = players.zip(first8.chunked(8))
//                        .groupByTo(mutableMapOf(), { z -> z.first }, {z -> z.second.toMutableList()} )
                        .associateBy({ z -> z.first }, { z -> z.second.toMutableList() })
                    sendStage(Stage.EIGHT_CARDS)
                }

                transitionOn<AckEvent> {
                    targetState = { preSchupf }
                    guard = { this@addInitialState.ack.containsAll(players) }
                }

//                transitionOn<TichuEvent> {
//                    onTriggered {  }
//                    targetState = { preSchupf }
//                }

            }

            addState(preSchupf) {
                onEntry {
                    for ((k, v) in players.zip((last6).chunked(6))) {
                        cardMap[k]!!.addAll(v)
                    }
                    sendStage(Stage.PRE_SCHUPF)
                }
                transition<AckEvent> {
                    onTriggered { println("preSchupf Trans") }
                    targetState = schupf
                    guard = { this@addState.ack.containsAll(players) }
                }
            }

            val selectStartPlayer = choiceState {
                val key = cardMap
                    .filter { it.value.contains(MAH) }
                    .map { it.key }
                    .first()
                playerStates[key]!!
            }

            playerStates.values.forEach() { ps ->
                addState(ps)
                {
                    onEntry {
                        sendTable(this.player)
                    }

                    transition<DogMoveEvent> {
                        targetState = playerStates[nextPlayer(ps, 2)]
                    }

                    transition<RegularMoveEvent> {
                        guard = { event.player == this@addState.player }
                        onTriggered {
                            cardMap.getValue(it.event.player)
                                .removeAll(it.event.cards)
                            table.add(Played(it.event.player, it.event.cards.toList()))
                            if (table.takeLast(3).all { it.cards.isEmpty() }
                            ) {
                                tricks.add(table.toList())
                                table = ArrayList()
                            }
                        }
                        targetState = playerStates[nextPlayer(ps)]
                        // todo: guard only react to events from the correct player
                    }
                    transitionOn<BombMoveEvent> {
                        // todo: go to player who played the bomb
                        targetState = { playerStates.getValue(event.player) }
                    }
                }
            }

            addState(schupf) {
                onEntry {
                    println("hello")
                    sendStage(Stage.SCHUPF)
                }
                transition<SchupfEvent> {
                    onTriggered { this@addState.schupfBuffer.put(it.event.user, it.event.cards) }
                    targetState = schupfed
                    guard = { this@addState.schupfBuffer.size == 4 }
//                    targetState = { if (schupf.players.size == 4) postSchupf else schupf }
                }
                // sends schupfed cards
                onExit { switchCards(schupfBuffer) }
            }

            addState(schupfed) {
                transition<AckEvent> {
                    targetState = postSchupf
                    guard = { this@addState.ack.size == 4 }
                }
            }

            addState(postSchupf) {
                onEntry {
                    sendStage(Stage.POST_SCHUPF)
                }
                transition<AckEvent> {
                    targetState = selectStartPlayer
                    guard = { this@addState.ack.size == 4 }
                }
            }

            onFinished { println("finished") }
        }
    }

    private fun sendTable(player: Player) {
        sendMessage(WrappedServerMessage(player, MakeYourMove(cardMap[player]!!, table)))
        // in theory we could use topic or so...
        // but boah...
        players.filter { it != player }
            .forEach {
                sendMessage(WrappedServerMessage(it, WhosTurn(player, cardMap.getValue(it), table)))
            }
    }

    private fun sendStage(stage: Stage) {
        for ((u, c) in cardMap) {
            val message = AckGameStage(stage, c)
            sendMessage(WrappedServerMessage(u, message))
        }
    }

    private fun nextPlayer(it: PlayerState, step: Int = 1): Player {
        val nextIdx = ((players.indexOf(it.player)) + step) % players.size
        return players[nextIdx]
    }

    private fun switchCards(schupfBuffer: MutableMap<Player, Map<Player, HandCard>>) {

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

    fun ack(u: Player, s: Ack) {
        when (s) {
            // todo: more consistent naming
            is Ack.BigTichu -> bTichu.ack.add(u)
            is Ack.TichuBeforeSchupf -> preSchupf.ack.add(u)
            is Ack.SchupfcardReceived -> schupfed.ack.add(u)
            is Ack.TichuBeforePlay -> postSchupf.ack.add(u)
        }
        machine.processEventBlocking(AckEvent)
    }

    // todo: make legality checker accept move / bomb
    fun move(player: Player, move: Move) {
        // check cards of player belong to it
        // todo: logic should probably be all in state machine
        /// but hey... as long as it is in a separate function
        val playerCards = cardMap[player]!!

        if (table.isNotEmpty()) {
            val tablePlayer = table.last().player
            if (tablePlayer == player) {
                // can't beat your own trick with regular move
                return
            }
        }
        val res = playedCardsValid(
            if (table.isNotEmpty()) table.last().cards else listOf(),
            move.cards,
            playerCards
            // todo wish
        )

        if (res.type == LegalType.OK) {
            machine.processEventBlocking(RegularMoveEvent(player, move.cards))
        } else {
//            sendMessage(Error)
        }
    }


    fun schupf(payload: SchupfEvent) {
        schupf.schupfBuffer[payload.user] = payload.cards
        machine.processEventBlocking(payload)
    }

    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }

    fun receive(wrappedUserMessage: WrappedUserMessage) {
        // todo: shouldn't switching happen inside state machine?
        val u = wrappedUserMessage.u
        when (val m = wrappedUserMessage.message) {
            is Ack -> ack(u, m)
            is Bomb -> TODO()
            is GiftDragon -> TODO()
            is Move -> move(u, m)
            is Schupf -> schupf(mapSchupfEvent(u, m))
            is Wish -> TODO()
            BigTichu -> TODO()
            Tichu -> TODO()
        }
//        machine.processEventBlocking(wrappedUserMessage)
    }

    private fun mapSchupfEvent(u: Player, schupf: Schupf): SchupfEvent {
        val cards = mapOf(
            u.partner() to schupf.partner,
            u.li() to schupf.li,
            u.re() to schupf.re,
        )
        return SchupfEvent(u, cards)

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


data class Played(val player: Player, val cards: List<PlayCard>)