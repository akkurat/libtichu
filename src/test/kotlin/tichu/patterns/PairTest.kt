package tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Pair
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
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
    fun testPairWithPhx()
    {
        val input = listOf(J2, D2, S5, PHX, S10 )
        val out = Pair.allPatterns(input)
        val expect = setOf(
            Pair.of(J2, D2),
            Pair.of(J2, PHX.asPlayCard(2)),
            Pair.of(D2, PHX.asPlayCard(2)),
            Pair.of(S5, PHX.asPlayCard(5)),
            Pair.of(S10, PHX.asPlayCard(10)),
        )
        assertThat(out).containsAll(expect)
    }

}