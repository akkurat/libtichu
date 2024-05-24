package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.PlayRound.State.INIT
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType
import ru.nsk.kstatemachine.*


//
//class AckEvent(override val data: PlayerMessage) : DataEvent<PlayerMessage> {}

typealias MutableTricks = ArrayList<List<Played>>

class PlayRound(val com: Out, cardMap: Map<Player, List<HandCard>>) {

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
            if (table.isNotEmpty()) table.last { !it.pass }.cards else listOf(), move.cards, playerCards // todo wish
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
            }
        }
        if (finishedPlayers().size == 3) {
            endRound()
            return
        }
        if (allPass()) {
            sendTrick(player)
            endTrick()
        }
        currentPlayer = nextPlayer(player)
        sendTableAndHandcards(currentPlayer)
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
//        send
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
        return RoundInfo(tricks.map { Trick(it) }, initalCardMap, leftoverHandcards)
    }

}

data class RoundInfo(
    val tricks: Tricks,
    val initialCardmap: Map<Player, Collection<HandCard>>,
    val leftoverHandcards: Map<Player, Collection<HandCard>>,
) {

    val orderOfWinning = tricks.flatMap { it.playerFinished() }
    val tricksByPlayer: Map<Player, List<Trick>> = tricks.groupBy { it.pointOwner() }

    fun getPoints(): Map<Group, Int> {
        val cards = getCards()
        return cards.mapValues { (_, v) -> v.sumOf { c -> c.getPoints() } }
            .mapValues { (_, v) -> if (v == 100) 200 else v }
    }

    fun getCards(): Map<Group, List<HandCard>> {
        val (first, second) = orderOfWinning
        // double win
        if (first.group == second.group) {
            return mapOf(first.group to fulldeck, first.group.other() to listOf())
        } else {
            val third = orderOfWinning[2]
            val last = Player.entries.minus(setOf(first, second, third))[0]
            val cards: Map<Group, MutableList<HandCard>> =
                mapOf(Group.A to mutableListOf(), Group.B to mutableListOf())
            // first player keeps cards
            cards[first.group]!!.addAll(tricksByPlayer[first]!!.flatMap { it.allCards() })
            // last player tricks go to winner
            tricksByPlayer[last]?.let { cards[first.group]!!.addAll(it.flatMap { it.allCards() }) }
            // handcards of last player go to opposite team
            cards[last.group.other()]!!.addAll(leftoverHandcards[last]!!)

            cards[second.group]!!.addAll(tricksByPlayer[second]!!.flatMap { it.allCards() })
            cards[third.group]!!.addAll(tricksByPlayer[third]!!.flatMap { it.allCards() })

            return cards;
        }
    }
}


interface IPlayed {
    val player: Player
}

data class Played(override val player: Player, val cards: Collection<PlayCard> = listOf()) : IPlayed {
    constructor(player: Player, card: PlayCard) : this(player, listOf(card))

    val pass get() = cards.isEmpty()
}

data class PlayerFinished(override val player: Player) : IPlayed
data class Wished(override val player: Player, val value: Int) : IPlayed
data class DrgGift(override val player: Player, val to: Player) : IPlayed

class Trick(val moves: List<IPlayed>) {
    /**
     * points
     */
    fun pointOwner(): Player {
        // dragon or
        val drg = moves.filterIsInstance<DrgGift>().firstOrNull()
        return if (drg != null) {
            drg.to;
        } else {
            moves.filterIsInstance<Played>().last { !it.pass }.player
        }
    }

    fun playerFinished(): List<Player> {
        return moves.filterIsInstance<PlayerFinished>().map { it.player }
    }

    fun allCards(): List<PlayCard> = moves.filterIsInstance<Played>().flatMap { it.cards }
}