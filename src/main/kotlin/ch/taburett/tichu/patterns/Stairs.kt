package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard
import org.paukov.combinatorics3.Generator
import kotlin.math.abs

class Stairs private constructor(cards: List<PlayCard>) : TichuPattern(TichuPatternType.STAIRS, cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): Stairs? {
            if (cards.size % 2 != 0) {
                return null
            }

            val sortedcards = cards.sortedBy { it.getValue() }
            val sorted = sortedcards.map { it.getValue() }
            for (i in cards.indices step 2) {
                if (sorted[i] != sorted[i + 1]) {
                    return null
                }
                if (i < cards.size - 2)
                    if (abs(sorted[i + 1] - sorted[i + 2]) != 1.0) {
                        return null
                    }
            }
            return Stairs(sortedcards)
        }

        override fun allPatterns(cards: Collection<HandCard>, cardinality: Int?, incPhx: Boolean): Set<Stairs> {

            // todo: phx support
            // maybe a class for deck would be more appropriate than just a list?
            val cardByValue = cards.filterIsInstance<NumberCard>()
                .groupBy { it.getValue() }
                .filter { it.value.size >= 2 } // only pairs


            val sorted = cardByValue.keys.sorted()
            if (sorted.size >= 2) { // need at least two pairs for a stair

                val found = mutableSetOf<Stairs>()

                val buffer = mutableListOf<Double>()
                var last = sorted.first()
                buffer.add(last)

                fun addFromBuff() {
                    if (buffer.size >= 2) {
                        val multiple = buffer.map {
                            Generator.combination(cardByValue.getValue(it)).simple(2).stream().toList()
                        }
                        val concreteCards = Generator.cartesianProduct(*multiple.toTypedArray())
                        concreteCards.stream().forEach {
                            found.add(Stairs(it.flatten()))
                        }
                    }
                }

                for (height in sorted.drop(1)) {
                    if (height - last > 1) {
                        addFromBuff()
                        buffer.clear()
                    }
                    buffer.add(height)
                    last = height
                }
                addFromBuff()

                return found

            }
            return setOf();
        }
    }
}