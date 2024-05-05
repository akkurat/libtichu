package ch.taburett.tichu

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.MutableRound
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.Player.*
import ch.taburett.tichu.game.SchupfEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import ru.nsk.kstatemachine.activeStates

class MutableRoundTest {
    @Test
    fun test8Cards() {
        val round = MutableRound()

        val cardsCheck = round.cardMap.values
            .map { c -> Executable { assertEquals(8, c.size) } }
            .toList()

        assertAll(
            { println(round.machine.activeStates()) },
            { assertAll(cardsCheck) },
        )
    }

    @Test
    fun transtionToAllCards() {
        val round = MutableRound()
        round.players.forEach { a -> round.ack(a, "G8") }
        val cardsCheck = round.cardMap.values
            .map { c -> Executable { assertEquals(14, c.size) } }
            .toList()

        assertAll(
            {
                assertThat(round.cardMap.values
                    .flatMap { v -> v })
                    .hasSameElementsAs(fulldeck)
            },
            { assertAll(cardsCheck) })
        round.cardMap
    }


    @Test
    fun transtionPostSchupf() {
        val round = MutableRound()
//        round.machine.startFromBlocking(States.schupf)
        round.players.forEach { a -> round.ack(a, "G8") }
        round.players.forEach { a -> round.ack(a, "14") }
        // todo: where to check validity of schupf payload?
        // assumming it's only occuring deliberately
        //
        randomShupf(round, A1)
        val state1 = round.machine.activeStates()
        listOf(A2, B1, B2).forEach { p -> randomShupf(round, p) }

        val state2 = round.machine.activeStates()

        round.players.forEach { p -> round.ack(p, "PostSchupf") }
        val state3 = round.machine.activeStates()

        assertAll(
            { assertThat(state1).hasSameElementsAs(setOf(MutableRound.schupf)) },
            { assertThat(state2).hasSameElementsAs(setOf(MutableRound.postSchupf)) },
            { assertThat(state3).containsAnyElementsOf(round.playerStates.values) },
        )


    }

    private fun randomShupf(round: MutableRound, player: Player) {
        val input = round.cardMap.get(player)
        if (input != null) {
            val toSchupf = input.take(3)
            val otherPlayers = round.players.filter { p -> p != player }
            val cards = otherPlayers.zip(toSchupf).associateBy({ p -> p.first }, { p -> p.second })
            round.schupf(SchupfEvent(player, cards))
        } else {
            throw NullPointerException("")
        }
    }


}