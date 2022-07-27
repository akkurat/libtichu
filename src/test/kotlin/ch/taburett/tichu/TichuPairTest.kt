package ch.taburett.tichu

import ch.taburett.tichu.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TichuPairTest {
    @Test
    fun allPairs() {
        val input = listOf(J2, D2, S2, D11, P11, J14, D14, S14, P14)
        val out = TichuPair.allPatterns(input)
        println(out)
        val expect = setOf(
            TichuPair.of(J2, D2),
            TichuPair.of(J2, S2),
            TichuPair.of(D2, S2),
            TichuPair.of(D11, P11),
            TichuPair.of(J14, D14),
            TichuPair.of(J14, S14),
            TichuPair.of(J14, P14),
            TichuPair.of(D14, S14),
            TichuPair.of(D14, P14),
            TichuPair.of(S14, P14)
        )
        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun testshit()
    {
        val input = listOf(J2, D2, S5, PHX, S10 )
        val out = TichuPair.allPatterns( input )
        val expect = setOf( TichuPair.of(J2, D2), TichuPair.of(J2, PHX.asPlayCard(2)), TichuPair.of(D2, PHX.asPlayCard(2)) )
        assertThat(out).containsAll(expect)
    }

}