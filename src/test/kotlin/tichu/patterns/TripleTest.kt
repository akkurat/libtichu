package tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Triple
import org.assertj.core.api.Assertions
import kotlin.test.Test

internal class TripleTest {
    @Test
    fun allTriples() {
        val input = listOf(J2, D2, S2, D11, P11, J14, D14, S14, P14)
        val out = Triple.allPatterns(input)
        println(out)
        val expect = setOf(
            Triple.of(J2, D2, S2),
            Triple.of(J14, D14, S14),
            Triple.of(J14, D14, P14),
            Triple.of(J14, S14, P14),
            Triple.of(D14, S14, P14)
        )
        Assertions.assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testOnePhx() {
        val input = listOf(J2, D2, S5, PHX, S10)
        val out = Triple.allPatterns(input)
        val expect = setOf(Triple.of(J2, D2, PHX.asPlayCard(2)))
        Assertions.assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testMultiPhx() {
        val input = listOf(J2, D2, S2, S5, D5, PHX, S10)
        val out = Triple.allPatterns(input)
        val expect = setOf(
            Triple.of(J2, D2, S2),
            Triple.of(J2, D2, PHX.asPlayCard(2)),
            Triple.of(J2, S2, PHX.asPlayCard(2)),
            Triple.of(D2, S2, PHX.asPlayCard(2)),
            Triple.of(S5, D5, PHX.asPlayCard(5))
        )
        Assertions.assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testnone() {
        val input = listOf(J2, MAH, S5, PHX, S10)
        val out = Triple.allPatterns(input)
        Assertions.assertThat(out).hasSameElementsAs(listOf())
    }

}