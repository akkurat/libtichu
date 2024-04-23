package ch.taburett.tichu.cards

import java.util.*

fun main() {
    println(permutations(listOf("A", "B", "C")))

    println( permutations(listOf(1,2,3)))
}


fun <T> permutations(input: List<T>): List<List<T>> {
    val solutions = mutableListOf<List<T>>()
    permutationsRecursive(input, 0, solutions)
    return solutions
}


fun <T> permutationsRecursive(input: List<T>, index: Int, answers: MutableList<List<T>>) {
    if (index == input.lastIndex) answers.add(input.toList())
    for (i in index .. input.lastIndex) {
        Collections.swap(input, index, i)
        permutationsRecursive(input, index + 1, answers)
        Collections.swap(input, i, index)
    }
}


fun <T> pickK(input: List<T>, k: Int): Set<Set<T>> {
    if( k > input.size )
    {
        throw IllegalArgumentException("To pick k, k must be <= size ")
    }

    // cheap way: reuse permuations

    val indices = permutations( input.indices.toList() )

    val takes = indices.map { it.take(k).map{ input[it]}.toSet() }
        .toSet()

    return takes
}
