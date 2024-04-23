package ch.taburett.tichu

import ch.taburett.tichu.cards.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BombTest
{
    @Test
    fun rejectPhx() {
        val deck = listOf(PHX.asPlayCard(2), S2, D2, P2)
        val out = Bomb.pattern(deck)
        assertThat(out).isNull()
    }

    @Test
    fun okBomb() {
        val deck = listOf(S14, D14, J14, P14)
        val out = Bomb.pattern(deck)
        assertThat(out?.type).isEqualTo(TichuPattern.BOMB)
    }
}