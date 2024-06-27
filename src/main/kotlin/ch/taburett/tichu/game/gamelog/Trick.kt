package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.common.EPlayer

/**
 *  log
 *
 */
data class Trick(val moves: List<IPlayLogEntry>) {
    /**
     * points
     */
    val pointOwner: EPlayer
        get() {
            // dragon or
            val drg = moves.filterIsInstance<IPlayLogEntry.DrgGiftedEntry>().firstOrNull()
            return if (drg != null) {
                drg.to;
            } else {
                moves.filterIsInstance<IPlayLogEntry.RegularMoveEntry>().last().player
            }
        }

    val playerFinishedEntry: List<EPlayer>
        get() {
            return moves.filterIsInstance<IPlayLogEntry.PlayerFinishedEntry>().map { it.player }
        }

    val allCards: List<PlayCard>
        get() = moves.filterIsInstance<IPlayLogEntry.RegularMoveEntry>().flatMap { it.cards }

}