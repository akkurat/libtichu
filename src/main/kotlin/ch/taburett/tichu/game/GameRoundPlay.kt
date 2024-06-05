package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.RoundPlay.State.INIT
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.game.protocol.BigTichu
import ch.taburett.tichu.game.protocol.Tichu
import ch.taburett.tichu.patterns.LegalType
import org.jetbrains.annotations.VisibleForTesting


typealias MutableTricks = ArrayList<Trick>

class RoundPlay(val com: Out, cardMap: Map<Player, List<HandCard>>, val preparationInfo: PreparationInfo?) {

    enum class State { INIT, RUNNING, FINISHED }

    private val goneCards = mutableSetOf<PlayCard>()
    private lateinit var leftoverHandcards: Map<Player, List<HandCard>>

    var state = INIT


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


    val initialPlayer = cardMap
        .filterValues { it.contains(MAH) }
        .map { it.key }
        .first()
    var table = Table()


    private fun endTrick() {
        tricks.add(table.toTrick())
        table = Table()
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

    private fun nextPlayer(lastPlayer: Player, step: Int = 1, cnt: Int = 0): Player {
        if (cnt >= 3) {
//            endRound()
//            return lastPlayer
            throw IllegalStateException("game should have ended already")
        }
        val nextIdx = ((playerList.indexOf(lastPlayer)) + 1) % playerList.size
        val player = playerList[nextIdx]
        if (cardMap.getValue(player).isEmpty()) {
            return nextPlayer(player, 1, cnt + 1)
        } else {
            return player
        }
    }

    @VisibleForTesting
    internal fun determineCurrentPlayer(): Player {
        if (tricks.isEmpty()) {
            return if (table.moves.filter { it !is Wished }.isEmpty()) {
                initialPlayer
            } else {
                val lastPlayer = table.moves.last().player
                nextPlayer(lastPlayer)
            }
        } else {
            if (table.moves.filter { it !is Wished }.isEmpty()) {
                val lastMove = tricks.last().moves.last()
                if (lastMove is PlayLogEntry) {
                    if (lastMove.cards.contains(DOG)) {
                        return nextPlayer(lastMove.player, 2)
                    }
                }
                return nextPlayer(lastMove.player)
            } else {
                return nextPlayer(table.moves.last(::notWish).player)
            }
        }
    }

    private fun notWish(it: IPlayLogEntry): Boolean = it !is Wished

    @Synchronized
    internal fun sendTableAndHandcards() {
        // todo: use tableLog to determine next/current player
        if (cardMap.getValue(determineCurrentPlayer()).isEmpty() && !dragonGiftPending) {
            println("asdfsa")
        }
        if (table.moves.lastOrNull()?.player == determineCurrentPlayer()) {
            println("Whoaaaa...")
        }
        playerList.forEach { player ->
            val message = WhosMove(
                player, determineCurrentPlayer(),
                cardMap.getValue(player).toList(), table.immutable(), tricks.lastOrNull(),
                pendingWish, dragonGiftPending,
                cardMap.mapValues { it.value.size }, goneCards.toSet()
            )
            sendMessage(WrappedServerMessage(player, message))
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
            _regularMove(player, move.cards)
        } else {
            sendMessage(WrappedServerMessage(player, Rejected(res.message, move)))
        }
    }

    /**
     *
     */
    private fun _regularMove(player: Player, playedCards: Collection<PlayCard>) {
        if (player != determineCurrentPlayer()) {
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
                sendTableAndHandcards()
                return
            } else {
                endTrick()
            }
        }
        sendTableAndHandcards()
    }

    private fun removePlayedCards(player: Player, playedCards: Collection<PlayCard>): MutableList<HandCard> {
        val handCards = cardMap.getValue(player)
        goneCards.addAll(playedCards)
        handCards.removeAll(playedCards.map { it.asHandcard() })
        return handCards
    }

    private fun endRound() {
        endTrick()
        leftoverHandcards = cardMap.mapValues { (_, v) -> v.toList() }
        if (activePlayers().size == 2) {
//            println("double win")
            if (activePlayers().first().playerGroup != activePlayers().last().playerGroup) {
                println("but how?")
            }
        }
        state = State.FINISHED
//        send
    }

    private fun sendMessage(wrappedServerMessage: WrappedServerMessage) {
        com.send(wrappedServerMessage)
    }


    // todo: maybe almost better if done in game class?
    fun getRoundInfo(): RoundInfo {
        return RoundInfo(preparationInfo, tricks, initalCardMap, leftoverHandcards)
    }

    @Synchronized
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
            if (u == determineCurrentPlayer()) { // ok as regular move
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
    override fun toString(): String = "B:$player:$cards"

}


interface IPlayLogEntry {
    // ugly but still need to figure out subtypes cleanly
    // maybe afterall don't make multiple interfaces...
    // just have type and nullable cards and stuff
    val type: String
    val player: Player
}

// todo rename
data class PlayLogEntry(override val player: Player, val cards: Collection<PlayCard> = listOf()) : IPlayLogEntry {
    constructor(player: Player, card: PlayCard) : this(player, listOf(card))

    val pass get() = cards.isEmpty()
    override val type = "RegularMove"
    override fun toString(): String = "${player.name}:$cards"
}

data class PlayerFinished(override val player: Player) : IPlayLogEntry {
    override val type = "Finished"
    override fun toString(): String = "F:$player"
}

data class Tichu(override val player: Player) : IPlayLogEntry {
    override val type = "Tichu"
    override fun toString(): String = "t:$player"

}

data class BigTichu(override val player: Player) : IPlayLogEntry {
    override val type = "BigTichu"
    override fun toString(): String = "T:$player"

}

data class Wished(override val player: Player, val value: Int) : IPlayLogEntry {
    override val type = "Wished"
    override fun toString(): String = "w:$player:$value"

}

data class WishFullfilled(override val player: Player, val value: Int) : IPlayLogEntry {
    override val type = "WishFullfilled"
    override fun toString(): String = "W:$player:$value"

}

data class DrgGift(override val player: Player, val to: Player) : IPlayLogEntry {
    override val type = "DrgGift"
    override fun toString(): String = "D:$player->$to"

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