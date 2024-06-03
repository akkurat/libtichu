package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard
import kotlin.math.abs

class Stairs private constructor(cards: List<PlayCard>) : TichuPattern(TichuPatternType.STAIRS, cards) {
    companion object : PatternImplFactory {
        override fun pattern(cards: Collection<PlayCard>): TichuPattern? {
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

        override fun allPatterns(cards: Collection<HandCard>, cardinality: Int?): Set<TichuPattern> {

            // maybe a class for deck would be more appropriate than just a list?
           val values = cards.filterIsInstance<NumberCard>()
                .groupBy { it.getValue() }
                .filter { it.value.size >= 2 } // only pairs


            val sorted = values.keys.sorted()
            if( sorted.size >= 2) { // need at least two pairs for a stair

                val found = mutableSetOf<TichuPattern>()

                val buffer = mutableListOf<Double>()
                var last = sorted.first()
                buffer.add(last)
                for (height in sorted.drop(1)) {
                    if(height - last > 1) {
                        if( buffer.size >=2) {
                            // todo: this omits other combos so far
                            val concreteCards = buffer.flatMap { values.getValue(it).take(2) }
                            found.add(Stairs(concreteCards))
                        }
                        buffer.clear()
                    }
                    buffer.add(height)
                    last = height
                }
                if( buffer.size >=2) {
                    // todo: this omits other combos so far
                    val concreteCards = buffer.flatMap { values.getValue(it).take(2) }
                    found.add(Stairs(concreteCards))
                    buffer.clear()
                }
                return found

            }
            // todo: phx
            return setOf();
        }
    }
}