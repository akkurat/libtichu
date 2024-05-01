package ch.taburett.tichu.cards

import ch.taburett.tichu.cards.OneHeight.OneHeight
import org.paukov.combinatorics3.Generator

class TichuPair private constructor(cards: Collection<PlayCard>, private val height: Int) : TichuPattern(TichuPatternType.PAIR, cards),
    OneHeight {

    // todo: move to companion

    companion object : PatternImplFactory {

        fun of(c1: PlayCard, c2: PlayCard): TichuPair {
            return pattern(setOf(c1,c2) )!!
        }
        fun of(cards: Collection<PlayCard>): TichuPair {
            return pattern(cards)!!
        }
        override fun pattern(cards: Collection<PlayCard>): TichuPair? {
            if (cards.size == 2) {
                val allSameValue = allSameValue(cards)
                if (allSameValue!=null) {
                    return TichuPair(cards, allSameValue)
                }
            }
            return null
        }

        override fun allPatterns(cards: Collection<HandCard>): Set<TichuPair> {
            val groups = cards.filter { it is NumberCard || it is PhoenixPlaycard }
                .map { it as PlayCard }
                .groupBy { it.value() }
            val result = groups.values.filter { it.size >= 2 }
                .flatMap { Generator.combination(it).simple(2).map(Companion::of) }
                .toSet()

            if (cards.contains(PHX)) {
                return result + cards.filterIsInstance<NumberCard>()
                    .map { of(it, PHX.asPlayCard(it.value)) }
            }
            return result;
        }

    }

    override fun getHeight(): Int {
        return height
    }

}