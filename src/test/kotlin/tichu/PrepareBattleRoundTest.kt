package tichu

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.Player.*
import ch.taburett.tichu.game.PrepareRound.*
import ch.taburett.tichu.game.protocol.Ack
import ch.taburett.tichu.game.protocol.Schupf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.function.Executable
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test

val out = Out { println(it) }

class PrepareBattleRoundTest {


    @Test
    fun test8Cards() {

        val round = PrepareRound(out)
        round.start()

        val cardsCheck = round.cardMap.values
            .map { c -> Executable { assertEquals(8, c.size) } }
            .toList()

        assertAll(
            { println(round.currentState) },
            { assertAll(cardsCheck) },
        )
    }

    @Test
    fun transtionToAllCards() {
        val round = PrepareRound(out)
        round.start()
        playerList.forEach { a -> round.react(a, Ack.BigTichu()) }
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
        val round = PrepareRound(out)
        round.start()
        playerList.forEach { a -> round.react(a, Ack.BigTichu()) }
        playerList.forEach { a -> round.react(a, Ack.TichuBeforeSchupf()) }
        // todo: where to check validity of schupf payload?
        // assumming it's only occuring deliberately
        //
        randomShupf(round, A1)
        val state1 = round.currentState
        listOf(A2, B1, B2).forEach { p -> randomShupf(round, p) }

        val state2 = round.currentState

        playerList.forEach { p -> round.react(p, Ack.SchupfcardReceived()) }
        val state3 = round.currentState

        playerList.forEach { p -> round.react(p, Ack.TichuBeforePlay()) }

        assertAll(
            { assertThat(state1).isEqualTo(round.schupfState) },
            { assertThat(state2::class).isEqualTo(schupfed::class) },
            { assertThat(state3::class).isEqualTo(preGame::class) },
            { assertTrue("finished", round.isFinished) }
        )

    }

}

fun randomShupf(round: PrepareRound, player: Player) {
    val input = round.cardMap.get(player)
    if (input != null) {
        val toSchupf = input.take(3)
        round.react(player, Schupf(toSchupf[0], toSchupf[1], toSchupf[2]))
    } else {
        throw NullPointerException("")
    }
}
