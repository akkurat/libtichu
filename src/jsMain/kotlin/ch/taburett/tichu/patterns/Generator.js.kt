package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.PlayCard

actual fun <T> cartesianProduct(vararg args: List<T>): Iterable<List<T>> {
    TODO("Not yet implemented")
}

actual fun <T> combinationSimple(
    num: Int,
    list: Collection<T>,
): Iterable<List<T>> {
    TODO("Not yet implemented")
}

actual fun toSortedMap(_byValue: Map<Int, List<PlayCard>>): Map<Int, List<PlayCard>> {
//    _byValue.
    return _byValue.entries
        .sortedBy { it.key }
        .associate { it.toPair() }
}