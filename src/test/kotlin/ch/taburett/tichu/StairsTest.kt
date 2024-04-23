package ch.taburett.tichu

import ch.taburett.tichu.cards.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StairsTest {
    @Test
    fun testStairsValid() {
        val cards = listOf( J2, J3, D3, P2)
        assertNotNull(Stairs.pattern(cards))
    }
    @Test
    fun testStairsValidPhx() {
        val cards = listOf( J2, J3, D3, PHX.asPlayCard(2))
        assertNotNull(Stairs.pattern(cards))
    }
    @Test
    fun testStairsInValid() {
        val cards = listOf( J2, J3, D3, P2, PHX.asPlayCard(2))
        assertNull(Stairs.pattern(cards))
    }
    @Test
    fun testStairsInValid2() {
        val cards = listOf(  MAJ, D3, P2, PHX.asPlayCard(2))
        assertNull(Stairs.pattern(cards))
    }
}