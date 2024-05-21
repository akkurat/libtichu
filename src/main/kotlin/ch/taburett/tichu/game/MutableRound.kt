package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.MutableRound.State.INIT
import ch.taburett.tichu.patterns.LegalType
import ru.nsk.kstatemachine.*


//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}

typealias MutableTricks = ArrayList<List<Played>>

class MutableRound(val com: Out, cardMap: Map<Player, List<out HandCard>>) {

    class TichuEvent(player: Player) : Event

    class BombMoveEvent(val player: Player) : Event

    enum class State { INIT, RUNNING, FINISHED }

    var state = INIT

    var table = ArrayList<Played>()

    var tricks = MutableTricks()

    lateinit var dragonGift: Player
    // todo  big tichus and such

    var currentPlayer: Player
    val cardMap: Map<Player, MutableList<HandCard>> = cardMap.mapValues { (_, l) -> l.toMutableList() }

    init {
        currentPlayer = cardMap
            .filter { it.value.contains(MAH) }
            .map { it.key }
            .first()
    }

    private fun endTrick() {
        tricks.add(table.toList())
        table = ArrayList()
    }

    fun start() {
        // todo: make enum
        if (state != INIT) {
            throw IllegalStateException("running or finished")
        }
        state = State.RUNNING
        sendTableAndHandcards(currentPlayer)
    }

    private fun activePlayers(): Int {
        return cardMap.values.count { it.isNotEmpty() }
    }

    private fun checkTricks(value: ArrayList<List<Played>>) {
        // iterate through checks basically via statemachine without being connected to outside
        // idea: create shadow round, play through and crate real one afterwards in order not to have
        // a faulty state (i.e. fail fast)
    }

    internal fun sendTableAndHandcards(player: Player) {
        sendMessage(WrappedServerMessage(player, MakeYourMove(cardMap[player]!!, table)))
        // in theory we could use topic or so...
        // but boah...
        playerList.filter { it != player }
            .forEach {
                sendMessage(WrappedServerMessage(it, WhosTurn(player, cardMap.getValue(it), table)))
            }
    }

    private fun sendTrick(player: Player) {
        playerList.forEach {
            sendMessage(WrappedServerMessage(it, WhosTurn(player, cardMap.getValue(it), table.toList())))
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
                //
                sendMessage(WrappedServerMessage(player, Rejected("can't beat your own trick with regular move", move)))
                return
            }
        }
        val res = playedCardsValid(
            if (table.isNotEmpty()) table.first().cards else listOf(), move.cards, playerCards // todo wish
        )

        if (res.type == LegalType.OK) {
            if (move.cards.contains(DOG)) {
                dogMove(player)
            } else {
                // maybe just return action instead of void functions?
                // would probably be easier to debug
                _move(player, move.cards)
            }
        } else {
            sendMessage(WrappedServerMessage(player, Rejected(res.message, move)))
        }
    }

    private fun _move(player: Player, cards: Collection<PlayCard>) {
        if (player != currentPlayer) {
            sendMessage(WrappedServerMessage(player, Rejected("not your turn yet")))
            return
        }
        val playerCards = cardMap.getValue(player)
        playerCards.removeAll(cards.map { it.asHandcard() })
        table.add(Played(player, cards.toList()))

        // that must be possible more easily
        if (cardMap.values.count { it.isEmpty() } == 3) {
            endRound()
            return
        } else {
            if (table.takeLast(activePlayers() - 1).all { it.cards.isEmpty() }) {
                endTrick()
            }
            currentPlayer = nextPlayer(player)
            sendTableAndHandcards(currentPlayer)
        }
    }

    private fun endRound() {
        endTrick()
        state = State.FINISHED
        // todo: handcards
    }

    private fun _bomb() {

    }


    private fun dogMove(player: Player) {
        cardMap.getValue(player).remove(DOG)
        currentPlayer = nextPlayer(player, 2)
        sendTableAndHandcards(currentPlayer)
    }


    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }

    fun getRoundInfo(): RoundInfo {
        // todo: tichu, drgn and so on
        return RoundInfo(tricks.toList())
    }

}

data class RoundInfo(val tricks: Tricks) {
    // todo count
}


data class Played(val player: Player, val cards: List<PlayCard>)