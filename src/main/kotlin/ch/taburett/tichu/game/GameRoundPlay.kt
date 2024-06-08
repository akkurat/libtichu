package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.RoundPlay.State.INIT
import ch.taburett.tichu.game.protocol.*
import ch.taburett.tichu.game.protocol.BigTichu
import ch.taburett.tichu.game.protocol.Tichu
import ch.taburett.tichu.patterns.LegalType
import org.jetbrains.annotations.VisibleForTesting


class RoundPlay(val com: Out, cardMap: Map<Player, List<HandCard>>, val preparationInfo: PreparationInfo?) {

    enum class State { INIT, RUNNING, FINISHED }

    private lateinit var leftoverHandcards: Map<Player, List<HandCard>>

    var state = INIT


    //todo: class
    var tricks = Tricks()

    val mutableDeck = MutableDeck(cardMap)

    val initalCardMap = cardMap.mapValues { (_, v) -> v.toList() }


    // todo: init by external log
    // also take protocol of schupf and so on into acccount...
    val tichuAnnouncements = mutableMapOf<Player, TichuType>()

    /**
     * Can be reset by bomb or actually gifting
     */
    var dragonGiftPending = false
    var pendingWish: Int? = null


    fun start() {
        // todo: make enum
        if (state != INIT) {
            throw IllegalStateException("running or finished")
        }
        state = State.RUNNING
        sendTableAndHandcards()
    }


    @VisibleForTesting
    internal fun determineCurrentPlayer(): Player {
        return tricks.nextPlayer(mutableDeck)
    }

    @Synchronized
    internal fun sendTableAndHandcards() {
        // todo: use tableLog to determine next/current player
        playerList.forEach { player ->
            val message = WhosMove(
                player, determineCurrentPlayer(),
                mutableDeck.cards(player), tricks.table, tricks.tricks.lastOrNull(),
                pendingWish, dragonGiftPending,
                mutableDeck.deckSizes(), mutableDeck.goneCards()
            )
            sendMessage(WrappedServerMessage(player, message))
        }
    }


    fun regularMove(player: Player, move: Move) {

        // TODO: make all logic external
        val playerCards = mutableDeck.cards(player)

        if (tricks.table.isNotEmpty()) {
            val tablePlayer = tricks.table.toBeat().player
            if (tablePlayer == player) {
                //
                sendMessage(WrappedServerMessage(player, Rejected("can't beat your own trick with regular move", move)))
                return
            }
        }

        val res = playedCardsValid(
            tricks.table.toBeatCards(),
            move.cards,
            playerCards,
            pendingWish
        )

        if (move.wish != null) {
            if (move.cards.contains(MAH)) {
                if (2 <= move.wish && move.wish <= 14) {
                    pendingWish = move.wish
                    tricks.add(Wished(player, pendingWish!!))
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
                tricks.add(WishFullfilled(player, pendingWish!!))
                pendingWish = null
            }
        }
        if (handCards.isEmpty()) {
            tricks.add(PlayerFinished(player))
        }

        tricks.add(PlayLogEntry(player, playedCards.toList()))
        if (playedCards.contains(DOG)) {
            tricks.endTrick()
        }

        if (mutableDeck.roundEnded()) {
            endRound()
        }

        if (tricks.table.allPass(mutableDeck.activePlayers())) {
            if (tricks.table.toBeatCards().contains(DRG)) {
                dragonGiftPending = true
                // shouldn't be necessary? actually always the one who wins with dragon its turn ... hm...
                // aaaah. reason is that one can finish with the dragon but still needs to gift it
                sendTableAndHandcards()
                return
            } else {
                tricks.endTrick()
            }
        }
        sendTableAndHandcards()
    }

    private fun removePlayedCards(player: Player, playedCards: Collection<PlayCard>): List<HandCard> {
        mutableDeck.playCards(player, playedCards)
        return mutableDeck.cards(player)
    }

    private fun endRound() {
        tricks.endTrick()
        leftoverHandcards = mutableDeck.leftovers()
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
        if (tricks.table.isEmpty()) {
            if (u == determineCurrentPlayer()) { // ok as regular move
                _regularMove(u, m.cards)
            } else {
                sendMessage(WrappedServerMessage(u, Rejected("Cannot bomb ausspiel", m)))
            }
        } else {
            val beats = m.pattern.beats(pattern(tricks.table.toBeatCards()))
            if (beats.type == LegalType.OK) {
                dragonGiftPending = false
                tricks.add(BombPlayed(u, m.cards))
                removePlayedCards(u, m.cards)
                tricks.endTrick()
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
            tricks.add(DrgGift(u, to))
            dragonGiftPending = false
            tricks.endTrick()
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

