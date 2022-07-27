import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

internal class RuelleTest {

    @Test
    fun findRuelleWithoutPhx() {
        val deck = listOf(MAJ, S2, D2, S3, P3, D4, P5, J8, J9, D10, P11, S12, P13, D14)
        val out = Ruelle.allPatterns(deck)

        val expect = setOf(
            Ruelle(MAJ, D2, S3, D4, P5),
            Ruelle(MAJ, D2, P3, D4, P5),
            Ruelle(MAJ, S2, S3, D4, P5),
            Ruelle(MAJ, S2, P3, D4, P5),
            Ruelle(J8, J9, D10, P11, S12, P13, D14)
        )

        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun findRuelleWithPhx() {
        val deck = listOf(PHX, MAJ, S2, S3, P3, D4, P5, D7, J8, J9)
        val out = Ruelle.allPatterns(deck)
        val expect = setOf(
            Ruelle(MAJ, S2, S3, D4, P5, PHX.asPlayCard(6), D7, J8, J9),
            Ruelle(MAJ, S2, P3, D4, P5, PHX.asPlayCard(6), D7, J8, J9)
        )


        assertThat(out).hasSameElementsAs(expect)
    }
}