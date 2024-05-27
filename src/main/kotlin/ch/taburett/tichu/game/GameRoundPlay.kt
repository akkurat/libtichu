package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.RoundPlay.State.INIT
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType


typealias MutableTricks = ArrayList<Trick>

class RoundPlay(val com: Out, cardMap: Map<Player, List<HandCard>>, val preparationInfo: PreparationInfo?) {

    enum class State { INIT, RUNNING, FINISHED }

    private lateinit var leftoverHandcards: Map<Player, List<HandCard>>

    var state = INIT

    var table: Table

    var tricks = MutableTricks()

    val initalCardMap = cardMap.mapValues { (_, v) -> v.toList() }
    val cardMap: Map<Player, MutableList<HandCard>> = cardMap.mapValues { (_, l) -> l.toMutableList() }

    // todo: init by external log
    // also take protocol of schupf and so on into acccount...
    val tichuAnnouncements = mutableMapOf<Player, TichuType>()

    /**
     * Can be reset by bomb or actually gifting
     */
    var dragonGiftPending = false
    var pendingWish: Int? = null

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

    private fun checkTricks(value: ArrayList<List<PlayLogEntry>>) {
        // iterate through checks basically via statemachine without being connected to outside
        // idea: create shadow round, play through and crate real one afterwards in order not to have
        // a faulty state (i.e. fail fast)
    }

    internal fun sendTableAndHandcards(player: Player, dragon: Stage = Stage.YOURTURN) {
        sendMessage(
            WrappedServerMessage(
                player,
                MakeYourMove(cardMap[player]!!, table, tricks.lastOrNull(), dragon, pendingWish)
            )
        )
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


    fun move(player: Player, move: Move) {

        // TODO: make all logic external
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
            playerCards,
            pendingWish
        )

        if (move.wish != null) {
            if (move.cards.contains(MAH)) {
                if (2 <= move.wish && move.wish <= 14) {
                    pendingWish = move.wish
                    table.add(Wished(player, pendingWish!!))
                } else {
                    sendMessage(WrappedServerMessage(player, Rejected("illegal wish range", move)))
                    return
                }
            } else {
                sendMessage(WrappedServerMessage(player, Rejected("can't wish without mah jong", move)))
                return
            }
        }

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
    private fun _move(player: Player, playedCards: Collection<PlayCard>) {
        if (player != table.currentPlayer) {
            sendMessage(WrappedServerMessage(player, Rejected("not your turn yet")))
            return
        }
        val handCards = cardMap.getValue(player)
        handCards.removeAll(playedCards.map { it.asHandcard() })
        if (pendingWish != null) {
            if (playedCards.filterIsInstance<NumberCard>().any { it.getValue() == pendingWish }) {
                table.add(WishFullfilled(player, pendingWish!!))
                pendingWish = null
            }
        }

        if (playedCards.isNotEmpty() && playedCards.first() == DRG) {
            dragonGiftPending = true
        }

        table.add(PlayLogEntry(player, playedCards.toList()))
        if (handCards.isEmpty()) {
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
            if (dragonGiftPending) {
                table.currentPlayer = nextPlayer(player)
                sendTableAndHandcards(table.currentPlayer, Stage.GIFT_DRAGON)
                return
            } else {
                sendTrick(player)
                endTrick()
            }
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
        table.add(PlayLogEntry(player, listOf(DOG)))
        cardMap.getValue(player).remove(DOG)
        table.currentPlayer = nextPlayer(player, 2)
        sendTableAndHandcards(table.currentPlayer)
    }


    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }


    // todo: maybe almost better if done in game class?
    fun getRoundInfo(): RoundInfo {
        return RoundInfo(preparationInfo, tricks, initalCardMap, leftoverHandcards)
    }

    fun receive(wrappedPlayerMessage: WrappedPlayerMessage) {
        val u = wrappedPlayerMessage.u
        when (val m = wrappedPlayerMessage.message) {
            is Move -> move(u, m)
            // wish async doesn't as you have to play the 1 in that trick
//            is Wish -> placeWish()
            BigTichu -> TODO()
            Tichu -> TODO()
            is GiftDragon -> giftDragon(u, m)
            else -> sendMessage(WrappedServerMessage(u, Rejected("Can't handle this message while playing", m)))
        }

    }

    private fun giftDragon(u: Player, m: GiftDragon) {
        val to = m.to.map(u)
        if (to.playerGroup != u.playerGroup) {
            table.add(DrgGift(u, to))
            dragonGiftPending = false
            endTrick()
            sendTableAndHandcards(table.currentPlayer)
        } else {
            sendMessage(WrappedServerMessage(u, Rejected("drg must be gifted to opponent", m)))
        }
    }
}


interface IPlayLogEntry {
    // ugly but still need to figure out subtypes cleanly
    // maybe afterall don't make multiple interfaces...
    // just have type and nullable cards and stuff
    val type: String
    val player: Player
}

data class PlayLogEntry(override val player: Player, val cards: Collection<PlayCard> = listOf()) : IPlayLogEntry {
    constructor(player: Player, card: PlayCard) : this(player, listOf(card))

    val pass get() = cards.isEmpty()
    override val type = "RegularMove"
}

data class PlayerFinished(override val player: Player) : IPlayLogEntry {
    override val type = "Finished"
}

data class Tichu(override val player: Player) : IPlayLogEntry {
    override val type = "Tichu"
}

data class BigTichu(override val player: Player) : IPlayLogEntry {
    override val type = "BigTichu"
}

data class Wished(override val player: Player, val value: Int) : IPlayLogEntry {
    override val type = "Wished"
}

data class WishFullfilled(override val player: Player, val value: Int) : IPlayLogEntry {
    override val type = "WishFullfilled"
}

data class DrgGift(override val player: Player, val to: Player) : IPlayLogEntry {
    override val type = "DrgGift"
}

/**
 *  log
 *
 */
data class Trick(val moves: List<IPlayLogEntry>) {
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
                moves.filterIsInstance<PlayLogEntry>().last { !it.pass }.player
            }
        }

    val playerFinished: List<Player>
        get() {
            return moves.filterIsInstance<PlayerFinished>().map { it.player }
        }

    val allCards: List<PlayCard>
        get() = moves.filterIsInstance<PlayLogEntry>().flatMap { it.cards }
}