package ch.taburett.tichu.game.core.preparation

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.ETichu
import ch.taburett.tichu.game.gamelog.IPlayLogEntry

data class PreparationInfo(
    val cards8: Map<EPlayer, List<HandCard>>,
    val cards6: Map<EPlayer, List<HandCard>>,
    val schupf: SchupfLog,
    val tichuLog: List<IPlayLogEntry>,
    val tichuMap: Map<EPlayer, ETichu>,
) {
    data class SchupfLog(
        val to: Map<EPlayer, Map<EPlayer, HandCard>>,
        val from: Map<EPlayer, Map<EPlayer, HandCard>>,
    )
}