package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.IPlayLogEntry.*
import ch.taburett.tichu.game.RoundPlay.State.INIT
import ch.taburett.tichu.game.protocol.Message.*
import ch.taburett.tichu.patterns.LegalType
import org.jetbrains.annotations.VisibleForTesting


class RoundPlay(
    val com: Out,
    deck: Deck,
    val preparationInfo: PreparationInfo?,
    soFar: ImmutableTricks?,
    override val name: String? = null,
) : TichuGameStage {
    constructor(
        com: Out,
        cardMap: Map<Player, Collection<HandCard>>,
        preparationInfo: PreparationInfo?,
        soFar: Tricks?, // hm.. how about outsourcing all logic inside a trick to the trick class?
        name: String? = null,
    ) : this(com, MutableDeck.createInitial(cardMap), preparationInfo, soFar, name)

    enum class State { INIT, RUNNING, FINISHED }

    private lateinit var leftoverHandcards: Map<Player, Collection<HandCard>>

    val tichuMap: PlayerETichuMutableMap =
        preparationInfo?.tichuMap?.toMutableMap() ?: Player.entries.associateWith { ETichu.NONE }.toMutableMap()

    var state = INIT

    var tricks = MutableTricks(soFar)

    val mutableDeck = MutableDeck.copy(deck)

    val initalCardMap = mutableDeck.getCardMap()


    // todo: init by external log
    // also take protocol of schupf and so on into acccount...

    /**
     * Can be reset by bomb or actually gifting
     */
    var dragonGiftPending = false
    var pendingWish: Int? = null


    fun start() {
        if (mutableDeck.roundEnded()) {
            endRound()
            return
        }
        if (tricks.table.allPass(mutableDeck.activePlayers())) {
//            println("shit")
            tricks.endTrick()
        }
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
                tricks.immutable(), pendingWish, dragonGiftPending,
                mutableDeck.deckSizes(),
                tichuMap.toMap(), mutableDeck.goneCards(),
            )
            sendMessage(WrappedServerMessage(player, message))
        }
    }


    fun regularMove(player: Player, move: Move) {

        // TODO: make all logic external
        val playerCards = mutableDeck.cards(player)

        if (tricks.table.moves.filterIsInstance<RegularMoveEntry>().isNotEmpty()) {
            val tablePlayer = tricks.table.toBeat()?.player
            if (tablePlayer == player) {
                //
                sendMessage(WrappedServerMessage(player, Rejected("can't beat your own trick with regular move", move)))
                return
            }
        } else {
            // todo...?
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
                    tricks.add(WishedEntry(player, pendingWish!!))
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
                tricks.add(WishFullfilledEntry(player, pendingWish!!))
                pendingWish = null
            }
        }
        if (handCards.isEmpty()) {
            tricks.add(PlayerFinishedEntry(player))
        }

        tricks.add(createMove(player, playedCards))
        if (playedCards.contains(DOG)) {
            tricks.endTrick()
        } else if (tricks.table.allPass(mutableDeck.activePlayers())) {
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

        if (mutableDeck.roundEnded()) {
            endRound()
        } else {
            sendTableAndHandcards()
        }
    }

    private fun createMove(
        player: Player,
        playedCards: Collection<PlayCard>,
    ) = if (playedCards.isEmpty() ) PassMoveEntry(player) else RegularMoveEntry(player, playedCards.toList())

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
        return RoundInfo(preparationInfo, tricks, initalCardMap, leftoverHandcards, tichuMap.toMap(), name)
    }

    @Synchronized
    fun receivePlayerMessage(wrappedPlayerMessage: WrappedPlayerMessage) {
        val u = wrappedPlayerMessage.u
        when (val m = wrappedPlayerMessage.message) {
            is Move -> regularMove(u, m)
            is Bomb -> bomb(u, m)
            // wish async doesn't as you have to play the 1 in that trick
//            is Wish -> placeWish()
            is Announce.SmallTichu ->
                if (mutableDeck.cards(u).size != 14) {
                    sendMessage(WrappedServerMessage(u, Rejected("Already cards played")))
                } else if (!tichuMap.replace(u, ETichu.NONE, ETichu.SMALL)) {
                    sendMessage(WrappedServerMessage(u, Rejected("Already a tichu announced")))
                } else {
                    if (name?.startsWith("Sim") == false) {
                        println("$name:$u small tichu in Round")
                    }
                    tricks.add(SmallTichuEntry(u))
                    sendTableAndHandcards()
                }

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
                tricks.add(BombEntry(u, m.cards))
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
            tricks.add(DrgGiftedEntry(u, to))
            dragonGiftPending = false
            tricks.endTrick()
            sendTableAndHandcards()
        } else {
            sendMessage(WrappedServerMessage(u, Rejected("drg must be gifted to opponent", m)))
        }
    }

    override fun toString(): String {
        return name ?: super.toString()
    }
}
