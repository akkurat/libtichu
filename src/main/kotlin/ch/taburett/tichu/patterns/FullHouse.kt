package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import org.paukov.combinatorics3.Generator

class FullHouse(val three: Collection<PlayCard>, val two: Collection<PlayCard>) :
    TichuPattern(TichuPatternType.FULLHOUSE, three + two) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
            if (cards.size == 5) {
                val gruoped = cards.groupBy { c -> c.getValue() }
                if (gruoped.size == 2) {
                    val size = gruoped.values.first().size
                    if (size == 3) {
                        return FullHouse(gruoped.values.first(), gruoped.values.last())
                    } else if (size == 2) {
                        return FullHouse(gruoped.values.last(), gruoped.values.first())
                    }
                }
            }
            return null
        }

        private fun allFullhouses(cards: Collection<PlayCard>): Set<TichuPattern> {
            val allPairs = Pair.allPatterns(cards, incPhx = true)
            val allTriples = Triple.allPatterns(cards, incPhx = true)

            return Generator.cartesianProduct(allTriples.toList(), allPairs.toList())
                .filter { it.first().getHeight() != it.last().getHeight() }
                .map { FullHouse(it.first().cards, it.last().cards) }
                .toSet()
        }

        override fun allPatterns(cards: Collection<HandCard>, cardinality: Int?, incPhx: Boolean): Set<TichuPattern> {
            if (incPhx && cards.contains(PHX)) {
                val numbers = cards.filterIsInstance<NumberCard>()
                    .map { it.getValue() }
                    .toSet()
                return numbers.flatMap { i ->
                    allFullhouses(cards.mapNotNull {
                        when (it) {
                            is Phoenix -> it.asPlayCard(i)
                            is NumberCard -> it
                            else -> null
                        }
                    })
                }.toSet()
            }

            return allFullhouses(cards.filterIsInstance<NumberCard>())

        }
    }

}