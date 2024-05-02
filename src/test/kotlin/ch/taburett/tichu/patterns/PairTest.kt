package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PairTest {
    @Test
    fun allPairs() {
        val input = listOf(J2, D2, S2, D11, P11, J14, D14, S14, P14)
        val out = Pair.allPatterns(input)
        println(out)
        val expect = setOf(
            Pair.of(J2, D2),
            Pair.of(J2, S2),
            Pair.of(D2, S2),
            Pair.of(D11, P11),
            Pair.of(J14, D14),
            Pair.of(J14, S14),
            Pair.of(J14, P14),
            Pair.of(D14, S14),
            Pair.of(D14, P14),
            Pair.of(S14, P14)
        )
        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testshit()
    {
        val input = listOf(J2, D2, S5, PHX, S10 )
        val out = Pair.allPatterns( input )
        val expect = setOf( Pair.of(J2, D2), Pair.of(J2, PHX.asPlayCard(2)), Pair.of(D2, PHX.asPlayCard(2)) )
        assertThat(out).containsAll(expect)
    }

}