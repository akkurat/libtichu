@file:JsExport()
package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import kotlin.js.JsExport
import kotlin.js.JsName

class Straight(cards: Iterable<PlayCard>) : TichuPattern(TichuPatternType.STRAIGHT, cards) {

    @JsName("byIterable")
    constructor(vararg cards: PlayCard) : this(cards.asIterable())

    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (!isValidRuelle(cards)) return null
            return Straight(cards)
        }

        fun isValidRuelle(cards: Collection<PlayCard>): Boolean {
            val sorted = cards.sortedBy { it.value() }
            for (i in 0 until sorted.size - 1) {
                if (sorted[i + 1].value() - sorted[i].value() != 1) {
                    return false
                }
            }
            return true
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPattern> {
            return wPhx(cards)
        }

        private fun wPhx(cards: Collection<HandCard>): Set<Straight> {
            val _byValue = cards.filter { it is NumberCard || it == MAJ }
                .map { it as PlayCard }
                .groupBy { it.value() }
//                .toSortedMap()
            val byValue = toSortedMap(_byValue)

            // step one: find possible flushes

            // 4 more cards needed for a ruel
            val heights = byValue.entries.toList()

            val lesStraights = ArrayList<Straight>()
            if (heights.size >= 5) {
                var i = 0;
                while (i < heights.size - 4) {
                    var phxAvailable = cards.contains(PHX)
                    val possibleRuelle = mutableListOf(heights[i].value)
                    i++
                    while (i < heights.size) {
                        val (height, cs) = heights[i]
                        val diff = height - heights[i - 1].key
                        if (diff == 1) {
                            possibleRuelle.add(cs)
                        } else if (diff == 2 && phxAvailable) {
                            phxAvailable = false
                            possibleRuelle.add(cs)
                            possibleRuelle.add(listOf(PHX.asPlayCard(height - 1)))
                        } else {
                            break
                        }
                        i++
                    }
                    if (possibleRuelle.size >= 4) {
                        if (phxAvailable) {
                            val phxValues = ArrayList<Int>()
                            val lastValue = possibleRuelle.last().first().value()
                            if (lastValue < S14.value()) {
                                phxValues.add(lastValue + 1)
                            }
                            val firstValue = possibleRuelle.first().first().value()
                            if (firstValue > MAJ.value()) {
                                phxValues.add(firstValue - 1)
                            }
                            possibleRuelle.add(phxValues.map { PHX.asPlayCard(it) })
                        }
                        if (possibleRuelle.size >= 5) {
                            val cartesianProduct = cartesianProduct(*possibleRuelle.toTypedArray())
                            val elements = cartesianProduct.map { Straight(it) }
                            lesStraights.addAll(elements)
                        }
                    }

                }
            }
            return lesStraights.toSet()
        }

    }

}
