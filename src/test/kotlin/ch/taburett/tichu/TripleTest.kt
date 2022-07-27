package ch.taburett.tichu

import ch.taburett.tichu.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class TripleTest {
    @Test
    fun allTriples() {
        val input = listOf(J2, D2, S2, D11, P11, J14, D14, S14, P14)
        val out = TichuTriple.allPatterns(input)
        println(out)
        val expect = setOf(
            TichuTriple.of(J2, D2, S2),
            TichuTriple.of(J14, D14, S14),
            TichuTriple.of(J14, D14, P14),
            TichuTriple.of(J14, S14, P14),
            TichuTriple.of(D14, S14, P14)
        )
        Assertions.assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testOnePhx() {
        val input = listOf(J2, D2, S5, PHX, S10)
        val out = TichuTriple.allPatterns(input)
        val expect = setOf(TichuTriple.of(J2, D2, PHX.asPlayCard(2)))
        Assertions.assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testMultiPhx() {
        val input = listOf(J2, D2, S2, S5, D5, PHX, S10)
        val out = TichuTriple.allPatterns(input)
        val expect = setOf(
            TichuTriple.of(J2, D2, S2),
            TichuTriple.of(J2, D2, PHX.asPlayCard(2)),
            TichuTriple.of(J2, S2, PHX.asPlayCard(2)),
            TichuTriple.of(D2, S2, PHX.asPlayCard(2)),
            TichuTriple.of(S5, D5, PHX.asPlayCard(5))
        )
        Assertions.assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testnone() {
        val input = listOf(J2, MAJ, S5, PHX, S10)
        val out = TichuTriple.allPatterns(input)
        Assertions.assertThat(out).hasSameElementsAs(listOf())
    }

}