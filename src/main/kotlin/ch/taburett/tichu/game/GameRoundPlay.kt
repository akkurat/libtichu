package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.RoundPlay.State.INIT
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.patterns.LegalType


typealias MutableTricks = ArrayList<Trick>

class RoundPlay(val com: Out, cardMap: Map<Player, List<HandCard>>, val preparationInfo: PreparationInfo?) {

    enum class State { INIT, RUNNING, FINISHED }

    private val goneCards = mutableSetOf<PlayCard>()
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
        sendTableAndHandcards()
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

    internal fun sendTableAndHandcards() {
        // in theory we could use topic or so...
        // but boah...
        playerList.forEach { player ->
            val message = WhosMove(player, table.currentPlayer,
                cardMap.getValue(player), table, tricks.lastOrNull(),
                pendingWish, dragonGiftPending,
                cardMap.mapValues { it.value.size },goneCards)
            sendMessage(WrappedServerMessage(player, message))
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


    fun regularMove(player: Player, move: Move) {

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
                _regularMove(player, move.cards)
            }
        } else {
            sendMessage(WrappedServerMessage(player, Rejected(res.message, move)))
        }
    }

    /**
     *
     */
    private fun _regularMove(player: Player, playedCards: Collection<PlayCard>) {
        if (player != table.currentPlayer) {
            sendMessage(WrappedServerMessage(player, Rejected("not your turn yet")))
            return
        }
        val handCards = removePlayedCards(player, playedCards)
        if (pendingWish != null) {
            if (playedCards.filterIsInstance<NumberCard>().any { it.getValue() - pendingWish!! == 0.0 }) {
                table.add(WishFullfilled(player, pendingWish!!))
                pendingWish = null
            }
        }

        table.add(PlayLogEntry(player, playedCards.toList()))
        if (handCards.isEmpty()) {
            table.add(PlayerFinished(player))
        }
        // todo: testroutine must also applied for dog and bomb
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
            if (table.toBeatCards().contains(DRG)) {
                dragonGiftPending = true
                // shouldn't be necessary? actually always the one who wins with dragon its turn ... hm...
                // aaaah. reason is that one can finish with the dragon but still needs to gift it
                table.currentPlayer = table.toBeat().player
                sendTableAndHandcards()
                return
            } else {
                sendTableAndHandcards()
                endTrick()
            }
        }
        // todo: logic inside table
        table.currentPlayer = nextPlayer(player)
        sendTableAndHandcards()
    }

    private fun removePlayedCards( player: Player, playedCards: Collection<PlayCard> ): MutableList<HandCard> {
        val handCards = cardMap.getValue(player)
        goneCards.addAll(playedCards)
        handCards.removeAll(playedCards.map { it.asHandcard() })
        return handCards
    }

    private fun endRound() {
        endTrick()
        leftoverHandcards = cardMap.mapValues { (_, v) -> v.toList() }
        state = State.FINISHED
//        send
    }

    private fun dogMove(player: Player) {
        table.add(PlayLogEntry(player, listOf(DOG)))
        removePlayedCards(player, listOf(DOG))
        table.currentPlayer = nextPlayer(player, 2)
        endTrick()
        sendTableAndHandcards()
    }


    fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }


    // todo: maybe almost better if done in game class?
    fun getRoundInfo(): RoundInfo {
        return RoundInfo(preparationInfo, tricks, initalCardMap, leftoverHandcards)
    }

    fun receivePlayerMessage(wrappedPlayerMessage: WrappedPlayerMessage) {
        val u = wrappedPlayerMessage.u
        when (val m = wrappedPlayerMessage.message) {
            is Move -> regularMove(u, m)
            is Bomb -> bomb(u, m)
            // wish async doesn't as you have to play the 1 in that trick
//            is Wish -> placeWish()
            BigTichu -> TODO()
            Tichu -> TODO()
            is GiftDragon -> giftDragon(u, m)
            else -> sendMessage(WrappedServerMessage(u, Rejected("Can't handle this message while playing", m)))
        }

    }

    private fun bomb(u: Player, m: Bomb) {
        if (table.isEmpty()) {
            if (u == table.currentPlayer) { // ok as regular move
                _regularMove(u, m.cards)
            } else {
                sendMessage(WrappedServerMessage(u, Rejected("Cannot bomb ausspiel", m)))
            }
        } else {
            val beats = m.pattern.beats(pattern(table.toBeatCards()))
            if (beats.type == LegalType.OK) {
                dragonGiftPending = false
                table.add(BombPlayed(u, m.cards))
                removePlayedCards(u, m.cards)
                endTrick()
                sendTableAndHandcards()
                // do bomb stuff
            } else {
                sendMessage(WrappedServerMessage(u, Rejected(beats.message, m)))
            }
        }
    }

    private fun giftDragon(u: Player, m: GiftDragon) {
        val to = m.to.map(u)
        if (to.playerGroup != u.playerGroup) {
            table.add(DrgGift(u, to))
            dragonGiftPending = false
            endTrick()
            sendTableAndHandcards()
        } else {
            sendMessage(WrappedServerMessage(u, Rejected("drg must be gifted to opponent", m)))
        }
    }
}

data class BombPlayed(override val player: Player, val cards: List<PlayCard>) : IPlayLogEntry {
    override val type = "Bomb"
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