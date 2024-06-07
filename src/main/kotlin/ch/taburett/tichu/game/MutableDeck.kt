package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.MAH
import ch.taburett.tichu.cards.PlayCard

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
}

class MutableDeck(cardMap: Map<Player, List<HandCard>>) : Deck {

    private val cardMap: Map<Player, MutableList<HandCard>> = cardMap.mapValues { (_, l) -> l.toMutableList() }
    private val _goneCards = mutableSetOf<PlayCard>()

    override val initialPlayer = cardMap
        .filterValues { it.contains(MAH) }
        .map { it.key }
        .first()


    override fun activePlayers(): Set<Player> {
        return cardMap.filter { (p, v) -> v.isNotEmpty() }.keys
    }

    override fun finishedPlayers(): Set<Player> {
        return cardMap.filter { (p, v) -> v.isEmpty() }.keys
    }

    override fun nextPlayer(lastPlayer: Player, step: Int, cnt: Int): Player {
        if (cnt >= 3) {
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

    override fun deckSizes() = cardMap.mapValues { it.value.size }
    override fun cards(player: Player): List<HandCard> = cardMap.getValue(player).toList()


    override fun roundEnded() = when (finishedPlayers().size) {
        2 -> {
            finishedPlayers().first().playerGroup == finishedPlayers().last().playerGroup
        }

        3 -> {
            true
        }

        else -> {
            false
        }
    }

    fun playCards(player: Player, playedCards: Collection<PlayCard>) {
        cardMap.getValue(player).removeAll(playedCards.map { it.asHandcard() })
        _goneCards.addAll(playedCards)
    }

    override fun leftovers(): Map<Player, List<HandCard>> = cardMap.mapValues { (_, v) -> v.toList() }
    override fun goneCards(): Set<PlayCard> = _goneCards
}
