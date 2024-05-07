package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.PlayCard
import org.paukov.combinatorics3.Generator

actual fun <T> cartesianProduct( vararg args: List<T>): Iterable<List<T>> {
    return Generator.cartesianProduct(args.toList() as Collection<List<T>>)
}

actual fun <T> combinationSimple(num: Int, list: Collection<T> ): Iterable<List<T>> {
    return Generator.combination(list).simple(num)
}

actual fun toSortedMap(_byValue: Map<Int, List<PlayCard>>): Map<Int, List<PlayCard>> {
    return _byValue.toSortedMap()
}