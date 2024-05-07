package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.PlayCard

expect fun <T> cartesianProduct(vararg args: List<T>): Iterable<List<T>>
expect fun <T> combinationSimple(num: Int, list: Collection<T>): Iterable<List<T>>


expect fun toSortedMap(_byValue: Map<Int, List<PlayCard>>): Map<Int,List<PlayCard>>

