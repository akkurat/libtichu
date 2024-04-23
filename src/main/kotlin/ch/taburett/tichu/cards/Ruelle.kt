package ch.taburett.tichu.cards

import org.paukov.combinatorics3.Generator

class Ruelle(cards: Iterable<PlayCard>) : ConcretePattern(TichuPattern.RUELLE, cards) {

    constructor(vararg cards: PlayCard) : this(cards.asIterable())

    companion object : PatternFactory {
        override fun pattern(cards: Collection<PlayCard>): ConcretePattern? {
            if (!isValidRuelle(cards)) return null
            return Ruelle(cards)
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

        override fun allPatterns(cards: Collection<HandCard>): Set<ConcretePattern> {
            return wPhx(cards)
        }

        private fun wPhx(cards: Collection<HandCard>): Set<Ruelle> {
            val byValue = cards.filter { it is NumberCard || it == MAJ }
                .map { it as PlayCard }
                .groupBy { it.value() }
                .toSortedMap()

            // step one: find possible flushes

            // 4 more cards needed for a ruel
            val heights = byValue.entries.toList()

            val lesRuelles = ArrayList<Ruelle>()
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
                            possibleRuelle.add( cs )
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
                            if (lastValue < S14.value) {
                                phxValues.add(lastValue + 1)
                            }
                            val firstValue = possibleRuelle.first().first().value()
                            if (firstValue > MAJ.value) {
                                phxValues.add(firstValue - 1)
                            }
                            possibleRuelle.add(phxValues.map { PHX.asPlayCard(it) })
                        }
                        if (possibleRuelle.size >= 5) {
                            val cartesianProduct = Generator.cartesianProduct(*possibleRuelle.toTypedArray())
                            val elements = cartesianProduct.map { Ruelle(it) }
                            lesRuelles.addAll(elements)
                        }
                    }

                }
            }

            return lesRuelles.toSet()
        }

    }

    override fun toString(): String {
        return cards.toString();
    }



}