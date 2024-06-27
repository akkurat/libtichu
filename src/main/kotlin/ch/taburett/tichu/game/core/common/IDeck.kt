package ch.taburett.tichu.game.core.common

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard

interface IDeck {
    val initialPlayer: EPlayer
    fun goneCards(): Set<PlayCard>
    fun activePlayers(): Set<EPlayer>
    fun finishedPlayers(): Set<EPlayer>
    fun nextPlayer(lastPlayer: EPlayer, step: Int = 1, cnt: Int = 0): EPlayer
    fun deckSizes(): Map<EPlayer, Int>
    fun cards(player: EPlayer): List<HandCard>
    fun roundEnded(): Boolean
    fun leftovers(): Map<EPlayer, List<HandCard>>
    fun getCardMap(): Map<EPlayer, List<HandCard>>
    fun copy(): IDeck
}