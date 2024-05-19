package tichu

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.Move
import ch.taburett.tichu.game.MutableRound
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.Player.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import ru.nsk.kstatemachine.activeStates
import ru.nsk.kstatemachine.startBlocking
import kotlin.test.Test
import kotlin.test.assertEquals


class MutableRoundTest {


    @Test
    fun testStartPlayer() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )
        val round = MutableRound(out, map)
        round.machine.startBlocking()
        assertThat(round.machine.activeStates().first()).isEqualTo(round.playerStates.getValue(A2))

    }

    @Test
    fun testFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )

        val round = MutableRound(out, map)

        round.machine.startBlocking()
        round.move(A2, Move(setOf(MAH)) )
        round.move(B2, Move(setOf(S2)) )
        round.move(A1, Move(setOf(J5)) )
        round.move(B1, Move(setOf(S7)) )
        round.move(A1, Move(setOf(PHX.asPlayCard(8))) )

        assertThat(round.machine.isFinished).isTrue()

    }


}