package tichu.patterns

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.patterns.Bomb
import ch.taburett.tichu.patterns.TichuPatternType
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
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
        assertThat(out?.type).isEqualTo(TichuPatternType.BOMB)
    }
}