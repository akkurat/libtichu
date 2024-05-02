package ch.taburett.tichu

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.TichuPatternType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable

internal class CardUtilsKtTest {
    @Test
    fun patternFh() {
        val p = pattern(listOf(J2, D2, J11, D11, PHX.asPlayCard(11)))
        assertNotNull(p)
        assertEquals(TichuPatternType.FULLHOUSE, p.type)
    }

    @Test
    fun patternSingle() {
        val p = pattern(listOf(PHX.asPlayCard(14)))
        assertNotNull(p)
        assertEquals(TichuPatternType.SINGLE, p.type)
    }

    @Test
    fun patternRelMajon() {
        val p = pattern(listOf(MAJ, S2, S3, D4, P5))
        assertNotNull(p)
        assertEquals(TichuPatternType.STRAIGHT, p.type)
    }

    @Test
    fun patternFhNot() {
        assertThrows<IllegalArgumentException> {
            pattern(listOf(MAJ, J2, D2, J11, D11, PHX.asPlayCard(11)))
        }
    }

    @Test
    fun patternSingleValid() {
        val execs = fulldeck
            .map { c -> if (c is Phoenix) c.asPlayCard(2) else c as PlayCard }
            .map {
                Executable {
                    val pattern = pattern(listOf(it))
                    assertNotNull(pattern)
                    assertEquals(TichuPatternType.SINGLE, pattern.type)
                }
            }

        assertAll(execs)

    }

    @Test
    fun pairValid() {
        val valid = listOf(listOf(PHX.asPlayCard(3), J3), listOf(S4, D4), listOf(P10, J10))
        assertAll(valid.map {
            Executable {
                val pattern = pattern(it)
                assertNotNull(pattern)
                assertEquals(TichuPatternType.PAIR, pattern.type)
            }
        })
    }

    @Test
    fun pheonixValue() {
        assertAll(
            { assertDoesNotThrow { PHX.asPlayCard(1) } },
            { assertDoesNotThrow { PHX.asPlayCard(14) } },
            { assertThrows<IllegalArgumentException> { PHX.asPlayCard(0) } },
            { assertThrows<IllegalArgumentException> { PHX.asPlayCard(17) } },
        )
    }


    @Test
    fun pairInvalid() {
        val inValid = listOf(listOf(S13, DRG), listOf(S4, D7), listOf(S4, MAJ))
        assertAll(inValid.map { Executable { assertThrows<IllegalArgumentException> { pattern(it) } } })
    }

    @Test
    fun tripleValid() {
        val valid = listOf(listOf(J2, D2, P2), listOf(S13, PHX.asPlayCard(13), P13))
        assertAll(valid.map { pattern(it) }
            .map {
                Executable { assertNotNull(it); assertEquals(it.type, TichuPatternType.TRIPLE) }
            })
    }

    @Test
    fun tripleInValid() {
        val inValid = listOf(listOf(J5, D2, P2), listOf(S14, PHX.asPlayCard(13), P13))
        assertAll(inValid.map { Executable { assertThrows<IllegalArgumentException> { pattern(it) } } })
    }

    @Test
    fun allPatterns() {
        val deck = listOf<HandCard>(MAJ, J2, D3, J3, P3, S3, D4, P5, J7, D8, S8, D11, J12, P14)
    }


}

