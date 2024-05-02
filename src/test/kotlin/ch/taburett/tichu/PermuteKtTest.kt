package ch.taburett.tichu

import ch.taburett.tichu.cards.pickK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PermuteKtTest
{
    @Test
    fun pickK_all(){
       val input = listOf("A", "B", "C", "D")
        val out = pickK(input, 4)
        assertEquals(setOf(input.toSet()), out)
    }

    @Test
    fun pickK_multi(){
        val input = listOf("A", "B", "C", "D")
        val out = pickK(input, 2)
        val expect = setOf(
            setOf("A", "B"),
            setOf("A", "C"),
            setOf("A", "D"),
            setOf("B", "C"),
            setOf("B", "D"),
            setOf("C", "D")
        )
        assertEquals(expect, out)
    }

    @Test
    fun pickK_multi2(){
        val input = listOf("A", "B", "C", "D")
        val out = pickK(input, 3)
        val expect = setOf(
            setOf("A", "B", "C"),
            setOf("A", "B", "D"),
            setOf("A", "C", "D"),
            setOf("B", "C", "D")
        )
        assertEquals(expect, out)
    }

    @Test
    fun pickK_one(){
        val input = listOf("A", "B", "C", "D")
        val out = pickK(input, 1)
        val expect = input.map { setOf(it) }.toSet()
        assertEquals( expect, out )
    }

    @Test
    fun verifyCounts() {

    }

}
