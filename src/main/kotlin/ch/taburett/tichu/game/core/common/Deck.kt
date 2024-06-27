package ch.taburett.tichu.game.core.common

import ch.taburett.tichu.cards.HandCard
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
    fun getCardMap(): Map<Player, List<HandCard>>
    fun copy(): Deck
}