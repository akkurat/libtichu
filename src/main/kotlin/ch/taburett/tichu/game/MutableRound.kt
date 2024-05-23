package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.MutableRound.State.INIT
import ch.taburett.tichu.patterns.LegalType
import ru.nsk.kstatemachine.*


//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}

typealias MutableTricks = ArrayList<List<Played>>

class MutableRound(val com: Out, cardMap: Map<Player, List<HandCard>>) {

    class TichuEvent(player: Player) : Event

    class BombMoveEvent(val player: Player) : Event

    enum class State { INIT, RUNNING, FINISHED }

    private lateinit var leftoverHandcards: Map<Player, List<HandCard>>
    var state = INIT

    var table = ArrayList<Played>()

    var tricks = MutableTricks()

    lateinit var dragonGift: Player
    // todo  big tichus and such

    var currentPlayer: Player
    val initalCardMap = cardMap.mapValues { (_, v) -> v.toList() }
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

    private fun activePlayers(): Set<Player> {
        return cardMap.filter { (p, v) -> v.isNotEmpty() }.keys
    }

    private fun finishedPlayers(): Set<Player> {
        return cardMap.filter { (p, v) -> v.isEmpty() }.keys
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

    /**
     *
     */
    private fun _move(player: Player, cards: Collection<PlayCard>) {
        if (player != currentPlayer) {
            sendMessage(WrappedServerMessage(player, Rejected("not your turn yet")))
            return
        }
        val playerCards = cardMap.getValue(player)
        playerCards.removeAll(cards.map { it.asHandcard() })
        table.add(Played(player, cards.toList()))

        if (finishedPlayers().size == 2) {
            if (finishedPlayers().first().group == finishedPlayers().last().group) {
                endRound()
                return
            } else if (finishedPlayers().size == 3) {
                endRound()
                return
            }
        } else {
            if (allPass()) {
                endTrick()
            }
            currentPlayer = nextPlayer(player)
            sendTableAndHandcards(currentPlayer)
        }
    }

    private fun allPass(): Boolean {

        val passedPlayers = mutableSetOf<Player>()

        for (p in table.reversed()) {
            if (p.pass) {
                passedPlayers.add(p.player)
            } else {
                val playerMove = p.player
                return passedPlayers.containsAll(activePlayers().minus(playerMove))
            }
        }

        return false

    }

    private fun endRound() {
        endTrick()
        leftoverHandcards = cardMap.mapValues { (_, v) -> v.toList() }
        state = State.FINISHED
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
        return RoundInfo(tricks.toList(), initalCardMap, leftoverHandcards)
    }

}

data class RoundInfo(
    val tricks: Tricks,
    val initialCardmap: Map<Player, List<HandCard>>,
    val leftoverHandcards: Map<Player, List<HandCard>>,
) {

    val orderOfWinning = tricks.flatten().filter { it.playerFinished }
    val tricksByPlayer: Map<Player, List<List<PlayCard>>> = tricks
        .map { it.filter { !it.pass } }
        .map { it.last().player to it.flatMap { p -> p.cards } }
        .groupBy({ it.first }, { it.second })

    fun getPoints(): Map<Group, Int> {
        val cards = getCards()
        return cards.mapValues { (_, v) -> v.sumOf { c -> c.getPoints() } }
            .mapValues { (_, v) -> if (v == 100) 200 else v }
    }

    fun getCards(): Map<Group, List<HandCard>> {
        val (first, second, third, last) = orderOfWinning.map { it.player }
        // double win
        if (first.group == second.group) {
            return mapOf(first.group to fulldeck, third.group to emptyList())
        } else {
            val cards: Map<Group, MutableList<HandCard>> =
                mapOf(Group.A to mutableListOf(), Group.B to mutableListOf())
            // first player keeps cards
            cards[first.group]!!.addAll(tricksByPlayer[first]!!.flatten())
            // last player tricks go to winner
            cards[first.group]!!.addAll(tricksByPlayer[last]!!.flatten())
            // handcards of last player go to opposite team
            cards[last.group.other()]!!.addAll(leftoverHandcards[last]!!)

            cards[second.group]!!.addAll(tricksByPlayer[second]!!.flatten())
            cards[third.group]!!.addAll(tricksByPlayer[third]!!.flatten())

            return cards;
        }
    }
    // todo count
}


data class Played(val player: Player, val cards: List<PlayCard>, val playerFinished: Boolean = false) {
    val pass get() = cards.isEmpty()
}