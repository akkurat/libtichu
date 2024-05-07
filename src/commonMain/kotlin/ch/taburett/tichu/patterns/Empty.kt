@file:JsExport()
package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import kotlin.js.JsExport

class Empty() : TichuPattern(TichuPatternType.EMPTY, setOf()) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.isEmpty()) {
                return Empty()
            }
            return null
        }
        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            return setOf()
        }
    }

}