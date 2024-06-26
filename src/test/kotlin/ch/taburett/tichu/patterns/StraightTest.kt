package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Straight
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import kotlin.test.Test

internal class StraightTest {

    @Test
    fun findRuelleWithoutPhx() {
        val deck = listOf(MAH, S2, D2, S3, P3, D4, P5, J8, J9, D10, P11, S12, P13, D14)
        val out = Straight.allPatterns(deck, incPhx = true)

        val expect = setOf(
            Straight(MAH, D2, S3, D4, P5),
            Straight(MAH, D2, P3, D4, P5),
            Straight(MAH, S2, S3, D4, P5),
            Straight(MAH, S2, P3, D4, P5),
            Straight(J8, J9, D10, P11, S12, P13, D14)
        )

        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun findRuelleWithPhx() {
        val deck = listOf(PHX, MAH, S2, S3, P3, D4, P5, D7, J8, J9)
        val out = Straight.allPatterns(deck, incPhx = true)
        val expect = setOf(
            Straight(MAH, S2, S3, D4, P5, PHX.asPlayCard(6), D7, J8, J9),
            Straight(MAH, S2, P3, D4, P5, PHX.asPlayCard(6), D7, J8, J9)
        )


        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun rejectRuelle() {
        val patterns = listOf(
            listOf(J2, J3), listOf(J2, J3, J4), listOf(J2, J3, J4, S5), listOf(J3, J4, S5)
        )
        assertAll(
            patterns.map { { assertThat(Straight.pattern(it)).isNull() } }
        )
    }


}