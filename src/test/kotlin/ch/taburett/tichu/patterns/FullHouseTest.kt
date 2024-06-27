package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.FullHouse
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

internal class FullHouseTest {
    @Test
    fun findAllFullhouses() {
        val deck = setOf(MAH, DRG, DOG, D2, S2, J2, J14, P14, S14, D6, J9)
        val out = FullHouse.allPatterns(deck, incPhx = true)

        val fhcards = setOf(D2, S2, J2, J14, P14, S14)
        val expect = setOf(
            FullHouse.pattern(fhcards - D2),
            FullHouse.pattern(fhcards - S2),
            FullHouse.pattern(fhcards - J2),
            FullHouse.pattern(fhcards - J14),
            FullHouse.pattern(fhcards - P14),
            FullHouse.pattern(fhcards - S14),
        )
        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun findAllFullhousesPhx() {
        val deck = setOf(S2, J2, D9, P9, PHX)
        val out = FullHouse.allPatterns(deck, incPhx = true)
        val fhcards = setOf(S2, J2, D9, P9)

        val expect = setOf(
            FullHouse.pattern(fhcards + PHX.asPlayCard(2)),
            FullHouse.pattern(fhcards + PHX.asPlayCard(9)),
        )
        assertThat(out).hasSameElementsAs(expect)
    }

    @Test
    fun findAllFullhousesPhxMore() {
        val deck = setOf(S2, J2, D2, D9, P9, PHX)
        val out = FullHouse.allPatterns(deck, incPhx = true)
        val fhcards = setOf(S2, J2, D2, D9, P9)

        val expect = setOf(
            FullHouse.pattern(fhcards - S2 + PHX.asPlayCard(2)),
            FullHouse.pattern(fhcards - D9 + PHX.asPlayCard(9)),
            FullHouse.pattern(setOf(S2, J2, D2, D9, P9)),
            FullHouse.pattern(setOf(S2, J2, PHX.asPlayCard(2), D9, P9)),
            FullHouse.pattern(setOf(S2, D2, PHX.asPlayCard(2), D9, P9)),
            FullHouse.pattern(setOf(J2, D2, PHX.asPlayCard(2), D9, P9)),
            FullHouse.pattern(setOf(S2, J2, D2, D9, PHX.asPlayCard(9))),
            FullHouse.pattern(setOf(S2, J2, D2, P9, PHX.asPlayCard(9))),
            FullHouse.pattern(setOf(D9, P9, PHX.asPlayCard(9), S2, J2)),
            FullHouse.pattern(setOf(D9, P9, PHX.asPlayCard(9), S2, D2)),
            FullHouse.pattern(setOf(D9, P9, PHX.asPlayCard(9), J2, D2))
        )
        assertThat(out).hasSameElementsAs(expect)
    }
}