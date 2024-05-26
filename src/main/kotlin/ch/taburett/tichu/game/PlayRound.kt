package ch.taburett.tichu.game

import ch.taburett.tichu.cards.DOG
import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.MAH
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.PlayRound.State.INIT
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType


typealias MutableTricks = ArrayList<Trick>

class PlayRound(val com: Out, cardMap: Map<Player, List<HandCard>>) {

    enum class State { INIT, RUNNING, FINISHED }

    private lateinit var leftoverHandcards: Map<Player, List<HandCard>>
    var state = INIT

    lateinit var table: Table

    var tricks = MutableTricks()

    val initalCardMap = cardMap.mapValues { (_, v) -> v.toList() }
    val cardMap: Map<Player, MutableList<HandCard>> = cardMap.mapValues { (_, l) -> l.toMutableList() }

    init {
        val currentPlayer = cardMap
            .filterValues { it.contains(MAH) }
            .map { it.key }
            .first()
        table = Table(currentPlayer)
    }

    private fun endTrick() {
        tricks.add(table.toTrick())
        table = Table(table.currentPlayer)
    }

    fun start() {
        // todo: make enum
        if (state != INIT) {
            throw IllegalStateException("running or finished")
        }
        state = State.RUNNING
        sendTableAndHandcards(table.currentPlayer)
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
        sendMessage(WrappedServerMessage(player, MakeYourMove(cardMap[player]!!, table, tricks.lastOrNull())))
        // in theory we could use topic or so...
        // but boah...
        playerList.filter { it != player }
            .forEach {
                sendCardsForUser(it, player)
            }
    }

    private fun sendTrick(player: Player) {
        playerList.forEach {
            sendCardsForUser(it, player)
        }
    }

    private fun sendCardsForUser(it: Player, player: Player) {
        sendMessage(WrappedServerMessage(it, WhosTurn(player, cardMap.getValue(it), table, tricks.lastOrNull())))
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
            val tablePlayer = table.toBeat().player
            if (tablePlayer == player) {
                //
                sendMessage(WrappedServerMessage(player, Rejected("can't beat your own trick with regular move", move)))
                return
            }
        }

        val res = playedCardsValid(
            table.toBeatCards(),
            move.cards,
            playerCards // todo wish
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
        if (player != table.currentPlayer) {
            sendMessage(WrappedServerMessage(player, Rejected("not your turn yet")))
            return
        }
        val playerCards = cardMap.getValue(player)
        playerCards.removeAll(cards.map { it.asHandcard() })
        table.add(Played(player, cards.toList()))
        if (playerCards.isEmpty()) {
            table.add(PlayerFinished(player))
        }

        if (finishedPlayers().size == 2) {
            if (finishedPlayers().first().playerGroup == finishedPlayers().last().playerGroup) {
                endRound()
                return
            }
        }
        // from here dragon matters

        if (finishedPlayers().size == 3) {
            endRound()
            return
        }
        if (table.allPass(activePlayers())) {
            sendTrick(player)
            endTrick()
        }
        // todo: logic inside table
        table.currentPlayer = nextPlayer(player)
        sendTableAndHandcards(table.currentPlayer)
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
        table.currentPlayer = nextPlayer(player, 2)
        sendTableAndHandcards(table.currentPlayer)
    }


    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }

    fun getRoundInfo(): RoundInfo {
        // todo: tichu, drgn and so on
        return RoundInfo(tricks, initalCardMap, leftoverHandcards)
    }
}


interface IPlayed {
    // ugly but still need to figure out subtypes cleanly
    // maybe afterall don't make multiple interfaces...
    // just have type and nullable cards and stuff
    val type: String
    val player: Player
}

data class Played(override val player: Player, val cards: Collection<PlayCard> = listOf()) : IPlayed {
    constructor(player: Player, card: PlayCard) : this(player, listOf(card))

    val pass get() = cards.isEmpty()
    override val type = "RegularMove"
}

data class PlayerFinished(override val player: Player) : IPlayed {
    override val type = "Finished"
}

data class Tichu(override val player: Player) : IPlayed {
    override val type = "Tichu"
}

data class BigTichu(override val player: Player) : IPlayed {
    override val type = "BigTichu"
}

data class Wished(override val player: Player, val value: Int) : IPlayed {
    override val type = "Wished"
}

data class DrgGift(override val player: Player, val to: Player) : IPlayed {
    override val type = "DrgGift"
}

/**
 *  log
 *
 */
data class Trick(val moves: List<IPlayed>) {
    /**
     * points
     */
    val pointOwner: Player
        get() {
            // dragon or
            val drg = moves.filterIsInstance<DrgGift>().firstOrNull()
            return if (drg != null) {
                drg.to;
            } else {
                moves.filterIsInstance<Played>().last { !it.pass }.player
            }
        }

    val playerFinished: List<Player>
        get() {
            return moves.filterIsInstance<PlayerFinished>().map { it.player }
        }

    val allCards: List<PlayCard>
        get() = moves.filterIsInstance<Played>().flatMap { it.cards }
}