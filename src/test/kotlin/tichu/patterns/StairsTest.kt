package tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Stairs
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StairsTest {
    @Test
    fun testStairsValid() {
        val cards = listOf(J2, J3, D3, P2)
        assertNotNull(Stairs.pattern(cards))
    }

    @Test
    fun testStairsValidPhx() {
        val cards = listOf(J2, J3, D3, PHX.asPlayCard(2))
        assertNotNull(Stairs.pattern(cards))
    }

    @Test
    fun testStairsInValid() {
        val cards = listOf(J2, J3, D3, P2, PHX.asPlayCard(2))
        assertNull(Stairs.pattern(cards))
    }

    @Test
    fun testStairsInValid2() {
        val cards = listOf(MAH, D3, P2, PHX.asPlayCard(2))
        assertNull(Stairs.pattern(cards))
    }

    @Test
    fun testFindSome() {
        val cards = listOf(D3, P2, J3, J2, P3, J4)
        assertEquals(
            setOf(
                pattern(listOf(D3, P2, J3, J2)),
                pattern(listOf(D3, P2, P3, J2)),
                pattern(listOf(P3, P2, J3, J2)),
            ),
            Stairs.allPatterns(cards)
        )
    }

    @Test
    fun testFindSome1() {
        val cards = listOf(D3, P2, J3, J2, P3, J4, S5, S4)
        assertEquals(
            setOf(
                pattern(listOf(D3, P2, J3, J2, J4,S4)),
                pattern(listOf(D3, P2, P3, J2, J4,S4)),
                pattern(listOf(P3, P2, J3, J2, J4,S4)),
            ),
            Stairs.allPatterns(cards)
        )
    }
}