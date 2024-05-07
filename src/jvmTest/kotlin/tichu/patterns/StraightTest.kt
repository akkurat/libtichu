package tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Straight
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

internal class StraightTest {

    @Test
    fun findRuelleWithoutPhx() {
        val deck = listOf(MAJ, S2, D2, S3, P3, D4, P5, J8, J9, D10, P11, S12, P13, D14)
        val out = Straight.allPatterns(deck)

        val expect = setOf(
            Straight(MAJ, D2, S3, D4, P5),
            Straight(MAJ, D2, P3, D4, P5),
            Straight(MAJ, S2, S3, D4, P5),
            Straight(MAJ, S2, P3, D4, P5),
            Straight(J8, J9, D10, P11, S12, P13, D14)
        )

        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun findRuelleWithPhx() {
        val deck = listOf(PHX, MAJ, S2, S3, P3, D4, P5, D7, J8, J9)
        val out = Straight.allPatterns(deck)
        val expect = setOf(
            Straight(MAJ, S2, S3, D4, P5, PHX.asPlayCard(6), D7, J8, J9),
            Straight(MAJ, S2, P3, D4, P5, PHX.asPlayCard(6), D7, J8, J9)
        )


        assertThat(out).hasSameElementsAs(expect)
    }
}