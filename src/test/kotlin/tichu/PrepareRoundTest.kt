package tichu

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.Player.*
import ch.taburett.tichu.game.PrepareRound.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.function.Executable
import ru.nsk.kstatemachine.activeStates
import ru.nsk.kstatemachine.startBlocking
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test

val out = Out { println(it) }

class PrepareRoundTest {


    @Test
    fun test8Cards() {


        val round = PrepareRound(out)
        round.machine.startBlocking()

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
        val round = PrepareRound(out)
        round.machine.startBlocking()
        playerList.forEach { a -> round.ack(a, Ack.BigTichu()) }
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
        val round = PrepareRound(out )
        round.machine.startBlocking()
        playerList.forEach { a -> round.ack(a, Ack.BigTichu()) }
        playerList.forEach { a -> round.ack(a, Ack.TichuBeforeSchupf()) }
        // todo: where to check validity of schupf payload?
        // assumming it's only occuring deliberately
        //
        randomShupf(round, A1)
        val state1 = round.machine.activeStates()
        listOf(A2, B1, B2).forEach { p -> randomShupf(round, p) }

        val state2 = round.machine.activeStates()

        playerList.forEach { p -> round.ack(p, Ack.SchupfcardReceived()) }
        val state3 = round.machine.activeStates()

        playerList.forEach { p -> round.ack(p, Ack.TichuBeforePlay()) }


        assertAll(
            { assertThat(state1).hasSameElementsAs(setOf(schupf)) },
            { assertThat(state2).hasSameElementsAs(setOf(schupfed)) },
            { assertThat(state3).hasSameElementsAs(setOf(preGame)) },
            { assertTrue( "finished", round.machine.isFinished )}
        )


    }



}
fun randomShupf(round: PrepareRound, player: Player) {
    val input = round.cardMap.get(player)
    if (input != null) {
        val toSchupf = input.take(3)
        val otherPlayers = playerList.filter { p -> p != player }
        val cards = otherPlayers.zip(toSchupf).associateBy({ p -> p.first }, { p -> p.second })
        round.schupf(SchupfEvent(player, cards))
    } else {
        throw NullPointerException("")
    }
}
