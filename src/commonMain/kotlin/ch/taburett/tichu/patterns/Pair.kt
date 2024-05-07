@file:JsExport
package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import kotlin.js.JsExport
import kotlin.js.JsName

class Pair private constructor(cards: Collection<PlayCard>, private val height: Int) : TichuPattern(
    TichuPatternType.PAIR, cards),
    OneHeight {

    // todo: move to companion

    companion object : PatternImplFactory {

        fun of(c1: PlayCard, c2: PlayCard): Pair {
            return pattern(setOf(c1,c2) )!!
        }
        @JsName("ofCollection")
        fun of(cards: Collection<PlayCard>): Pair {
            return pattern(cards)!!
        }
        override fun pattern(cards: Collection<PlayCard>): Pair? {
            if (cards.size == 2) {
                val allSameValue = allSameValue(cards)
                if (allSameValue!=null) {
                    return Pair(cards, allSameValue)
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<Pair> {
            val groups = cards.filter { it is NumberCard || it is PhoenixPlaycard }
                .map { it as PlayCard }
                .groupBy { it.value() }
            val result = groups.values.filter { it.size >= 2 }
                .flatMap { combinationSimple(2,it).map(Companion::of) }
                .toSet()

            if (cards.contains(PHX)) {
                return result + cards.filterIsInstance<NumberCard>()
                    .map { of(it, PHX.asPlayCard(it.value())) }
            }
            return result;
        }

    }

    override fun getHeight(): Int {
        return height
    }

}