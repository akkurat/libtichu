package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.LegalType
import ru.nsk.kstatemachine.*


//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}

typealias Tricks = ArrayList<List<Played>>

class MutableRound(val com: Out, var cardMap: Map<Player, MutableList<out HandCard>>) {

    class PlayerState(val player: Player) : DefaultState()

    object AckEvent : Event
    class TichuEvent(player: Player) : Event

    class RegularMoveEvent(val player: Player, val cards: Collection<PlayCard>) : Event
    class DogMoveEvent(val player: Player) : Event
    class BombMoveEvent(val player: Player) : Event

    val playerStates: Map<Player, PlayerState> = Player.entries.associateBy({ it }) { PlayerState(it) }

    var machine: StateMachine


    var table = ArrayList<Played>()

    var tricks = Tricks()


    // todo: split round logic and trick logic
    init {
        machine = createStdLibStateMachine("TStateRound", start = false)
        {

            val selectStartPlayer = choiceState {
                val key = cardMap
                    .filter { it.value.contains(MAH) }
                    .map { it.key }
                    .first()
                playerStates[key]!!
            }
//            addInitialState( selectStartPlayer )
            setInitialState(selectStartPlayer)

            val finish = finalState { }


            val endGame = choiceState {
                // round ends
                // todo game ends
                tricks.add(table.toList())
                table = ArrayList()
                finish
            }

            playerStates.values.forEach() { ps ->
                addState(ps)
                {
                    onEntry {
                        sendTable(this.player)
                    }

                    transition<DogMoveEvent> {
                        targetState = playerStates[nextPlayer(ps.player, 2)]
                        onTriggered {
                            val playerCards = cardMap.getValue(it.event.player)
                            playerCards.remove(DOG)
                        }
                    }

                    transitionConditionally<RegularMoveEvent> {
                        direction = {
                            if (event.player != this@addState.player) {
                                noTransition()
                            }
                            val playerCards = cardMap.getValue(event.player)
                            playerCards.removeAll(event.cards.map { it.asHandcard() })
                            table.add(Played(event.player, event.cards.toList()))

                            // that must be possible more easily
                            if (cardMap.values.count { it.isEmpty() } == 3) {
                                targetState(endGame)
                            } else {
                                if (table.takeLast(3).all { it.cards.isEmpty() }
                                ) {
                                    tricks.add(table.toList())
                                    table = ArrayList()
                                    stay()
                                } else {
                                    targetState(playerStates.getValue(nextPlayer(ps.player)))
                                }
                            }
                        }
                    }

                    transitionOn<BombMoveEvent> {
                        // todo: go to player who played the bomb
                        targetState = { playerStates.getValue(event.player) }
                    }
                    onExit {
                        sendTable(this.player)
                    }
                }
            }


            onFinished { println("finished") }
        }

    }

    private fun checkTricks(value: ArrayList<List<Played>>) {
        // iterate through checks basically via statemachine without being connected to outside
        // idea: create shadow round, play through and crate real one afterwards in order not to have
        // a faulty state (i.e. fail fast)
    }

    private fun sendTable(player: Player) {
        sendMessage(WrappedServerMessage(player, MakeYourMove(cardMap[player]!!, table)))
        // in theory we could use topic or so...
        // but boah...
        playerList.filter { it != player }
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

    private fun nextPlayer(player_in: Player, step: Int = 1, cnt: Int = 0): Player {
        if (cnt == 4) {
            throw IllegalStateException("Game is probably finished")
        }
        val nextIdx = ((playerList.indexOf(player_in)) + step) % playerList.size
        val player = playerList[nextIdx]
        if (cardMap.getValue(player).isEmpty()) {
            return nextPlayer(player, step, cnt + 1)
        } else {
            return player
        }
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
            if (move.cards.contains(DOG)) {
                machine.processEventBlocking(DogMoveEvent(player))
            } else {
                machine.processEventBlocking(RegularMoveEvent(player, move.cards))
            }
        } else {
//            sendMessage(Error)
        }
    }


    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
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