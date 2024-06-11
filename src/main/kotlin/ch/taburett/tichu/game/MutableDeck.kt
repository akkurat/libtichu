package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*

interface Deck {
    val initialPlayer: Player
    fun goneCards(): Set<PlayCard>
    fun activePlayers(): Set<Player>
    fun finishedPlayers(): Set<Player>
    fun nextPlayer(lastPlayer: Player, step: Int = 1, cnt: Int = 0): Player
    fun deckSizes(): Map<Player, Int>
    fun cards(player: Player): List<HandCard>
    fun roundEnded(): Boolean
    fun leftovers(): Map<Player, List<HandCard>>
    fun getCardMap(): Map<Player, List<HandCard>>
    fun copy(): Deck
}

class MutableDeck private constructor(
    _cardMap: Map<Player, Collection<HandCard>>,
    override val initialPlayer: Player,
    _goneCards: Collection<PlayCard>,
) : Deck {
    companion object {

        fun createInitial(cardMap: Map<Player, Collection<HandCard>>): MutableDeck {
            // logic later on works only if all cards in map are present
            val valid = fulldeckSet == cardMap.values.flatten().toSet()
            val initialPlayer = cardMap
                .filterValues { it.contains(MAH) }
                .map { it.key }
                .first()
            if (valid) {
                return MutableDeck(cardMap, initialPlayer, emptySet())
            } else {
                throw IllegalArgumentException("Card map must contain complete deck")
            }
        }

        fun createStarted(
            cardMap: Map<Player, Collection<HandCard>>,
            initialPlayer: Player,
            _goneCards: Collection<PlayCard>,
        ): MutableDeck {
            val deckVAlid = (cardMap.values.flatten() + _goneCards.map { it.asHandcard() }).toSet() == fulldeckSet
            if (deckVAlid) {
                return MutableDeck(cardMap, initialPlayer, _goneCards)
            } else {
                throw IllegalArgumentException("Card map and gone cards must add up to full deck")
            }
        }

        fun copy(deck: Deck): MutableDeck {
            return if (deck is MutableDeck) {
                deck.copy()
            } else {
                MutableDeck(deck.getCardMap(), deck.initialPlayer, deck.goneCards())
            }
        }

        fun createInitial(): MutableDeck {
            return createInitial(Player.entries.zip(fulldeck.shuffled().chunked(14)).toMap())
        }
    }

    private val cardMap: Map<Player, MutableList<HandCard>> = _cardMap.mapValues { (_, l) -> l.toMutableList() }
    private val _goneCards = _goneCards.toMutableSet() ?: mutableSetOf()


    override fun activePlayers(): Set<Player> {
        return cardMap.filter { (p, v) -> v.isNotEmpty() }.keys
    }

    override fun finishedPlayers(): Set<Player> {
        return cardMap.filter { (p, v) -> v.isEmpty() }.keys
    }

    override tailrec fun nextPlayer(lastPlayer: Player, step: Int, cnt: Int): Player {
        if (cnt >= 3) {
            throw IllegalStateException("game should have ended already")
        }
        val nextIdx = ((playerList.indexOf(lastPlayer)) + 1) % playerList.size
        val player = playerList[nextIdx]
        return if (cardMap.getValue(player).isEmpty()) {
            nextPlayer(player, 1, cnt + 1)
        } else {
            player
        }
    }

    override fun deckSizes() = cardMap.mapValues { it.value.size }
    override fun cards(player: Player): List<HandCard> = cardMap.getValue(player).toList()
    override fun getCardMap() = cardMap.mapValues { (_, v) -> v.toList() }

    override fun roundEnded(): Boolean {
        val size = finishedPlayers().size
        val s = when (size) {
            2 -> {
                finishedPlayers().first().playerGroup == finishedPlayers().last().playerGroup
            }

            3 -> {
                true
            }

            4 -> {
//                throw IllegalArgumentException("game was already finished ")
                true
            }

            else -> {
                false
            }
        }
//        if(s) {
//            println("$size $this")
//        }
        return s
    }

    fun playCards(player: Player, playedCards: Collection<PlayCard>) {
        cardMap.getValue(player).removeAll(playedCards.map { it.asHandcard() })
        _goneCards.addAll(playedCards)
    }

    override fun leftovers(): Map<Player, List<HandCard>> = cardMap.mapValues { (_, v) -> v.toList() }
    override fun goneCards(): Set<PlayCard> = _goneCards
    override fun copy(): MutableDeck {
        return MutableDeck(cardMap, initialPlayer, _goneCards)
    }
}
