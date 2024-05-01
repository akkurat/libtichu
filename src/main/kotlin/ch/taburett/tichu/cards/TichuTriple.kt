package ch.taburett.tichu.cards

import ch.taburett.tichu.cards.OneHeight.OneHeight
import org.paukov.combinatorics3.Generator

class TichuTriple private constructor(cards: Collection<PlayCard>, private val height: Int) :
    TichuPattern(TichuPatternType.TRIPLE, cards), OneHeight {


    companion object : PatternImplFactory {
        fun of(card1: PlayCard, card2: PlayCard, card3: PlayCard): TichuTriple {
            return of(setOf(card1, card2, card3))
        }

        fun of(cards: Collection<PlayCard>): TichuTriple {
            return pattern(cards)
                ?: throw IllegalArgumentException("All Cards must have same height")
        }

        override fun pattern(cards: Collection<PlayCard>): TichuTriple? {
            if (cards.size == 3) {
                val allSameValue = allSameValue(cards)
                if (allSameValue != null) {
                    return TichuTriple(cards, allSameValue);
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuTriple> {

            val groups = cards.filter{it is NumberCard || it is PlayCard }
                .map { it as PlayCard }
                .groupBy { it.value() }
            val numberPairs = groups.values.filter { it.size >= 3 }
                .flatMap { Generator.combination(it).simple(3).map { of(it) } }
                .toSet()

            if (cards.contains(PHX)) {
                return numberPairs + groups.filter { it.value.size >= 2 }
                    .flatMap { (k, v) ->
                        Generator.combination(v).simple(2).map {
                            of( it + PHX.asPlayCard(k) )
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