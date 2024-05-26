package tichu

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.protocol.Move
import ch.taburett.tichu.game.RoundPlay
import ch.taburett.tichu.game.Player.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import kotlin.test.Test


class RoundPlayTest {


    @Test
    fun testStartPlayer() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )
        val round = RoundPlay(out, map, null)
        round.start()
        assertThat(round.table.currentPlayer).isEqualTo(A2)

    }

    @Test
    fun testRejectWrongPlayer() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH, D2),
            B2 to mutableListOf(S2),
        )
        val round = RoundPlay(out, map, null)
        round.start()
        round.move(A1, Move(setOf(J5)))

        assertThat(round.table.currentPlayer).isEqualTo(A2)
    }

    @Test
    fun testTrickFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH, D2),
            B2 to mutableListOf(S2),
        )

        val round = RoundPlay(out, map, null)

        round.start()
        round.move(A2, Move(setOf(MAH)))
        round.move(B2, Move(setOf()))
        round.move(A1, Move(setOf()))
        round.move(B1, Move(setOf()))

        assertThat(round.table.currentPlayer).isEqualTo(A2)
    }

    @Test
    fun playerFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )

        val round = RoundPlay(out, map, null)

        round.start()
        round.move(A2, Move(setOf(MAH)))
        round.move(B2, Move(setOf(S2)))
        round.move(A1, Move(setOf()))
        round.move(B1, Move(setOf()))

        assertThat(round.table.currentPlayer).isEqualTo(A1)
    }

    @Test
    fun testFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )

        val round = RoundPlay(out, map, null)

        round.start()
        round.move(A2, Move(setOf(MAH)))
        round.move(B2, Move(setOf(S2)))
        round.move(A1, Move(setOf(J5)))
        round.move(B1, Move(setOf(S7)))
        round.move(A1, Move(setOf(PHX.asPlayCard(8))))

        assertAll(
            { assertThat(round.state == RoundPlay.State.FINISHED).isTrue() },
        )
    }


}