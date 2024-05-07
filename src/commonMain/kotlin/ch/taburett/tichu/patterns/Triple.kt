@file:JsExport()
package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import kotlin.js.JsExport
import kotlin.js.JsName

class Triple private constructor(cards: Collection<PlayCard>, private val height: Int) :
    TichuPattern(TichuPatternType.TRIPLE, cards), OneHeight {


    companion object : PatternImplFactory {
        fun of(card1: PlayCard, card2: PlayCard, card3: PlayCard): Triple {
            return of(setOf(card1, card2, card3))
        }

        @JsName("ofCollection")
        fun of(cards: Collection<PlayCard>): Triple {
            return pattern(cards)
                ?: throw IllegalArgumentException("All Cards must have same height")
        }

        override fun pattern(cards: Collection<PlayCard>): Triple? {
            if (cards.size == 3) {
                val allSameValue = allSameValue(cards)
                if (allSameValue != null) {
                    return Triple(cards, allSameValue);
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<Triple> {

            val groups = cards.filter { it is NumberCard || it is PlayCard }
                .map { it as PlayCard }
                .groupBy { it.value() }
            val numberPairs = groups.values.filter { it.size >= 3 }
                .flatMap { combinationSimple(3, it).map { of(it) } }
                .toSet()

            if (cards.contains(PHX)) {
                return numberPairs + groups.filter { it.value.size >= 2 }
                    .flatMap { (k, v) ->
                        combinationSimple(2,v).map {
                            of(it + PHX.asPlayCard(k))
                        }
                    }
            }
            return numberPairs;
        }

    }

    override fun getHeight(): Int {
        return height
    }

}