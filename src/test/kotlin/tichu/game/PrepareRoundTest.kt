package tichu.game

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.Player.*
import ch.taburett.tichu.game.PrepareRound.preGame
import ch.taburett.tichu.game.PrepareRound.schupfed
import ch.taburett.tichu.game.protocol.Message
import ch.taburett.tichu.game.protocol.Message.Ack
import ch.taburett.tichu.game.protocol.Message.Schupf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.function.Executable
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test

val out = Out { println(it) }

class PrepareRoundTest {


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

    @Test
    fun testCardsAvailable() {

        val outCollector = OutCollector()
        val round = PrepareRound(outCollector)
        round.start()
        playerList.forEach { a -> round.react(a, Ack.BigTichu()) }
        playerList.forEach { a -> round.react(a, Ack.TichuBeforeSchupf()) }
        // todo: where to check validity of schupf payload?
        // assumming it's only occuring deliberately
        //
        val cards = round.cardMap.getValue(B2)
        listOf(A1, A2).forEach { randomShupf(round, it) }
        round.react(B1, Schupf(cards[0], cards[1], cards[2]))
        round.react(B2, Schupf(cards[0], cards[0], cards[0]))

        assertThat(outCollector.list).containsSubsequence(
            WrappedServerMessage(B1, Message.Rejected("Cheating!")),
            (WrappedServerMessage(B2, Message.Rejected("Cheating!")))
        )
    }
}

class OutCollector : Out {
    val list = mutableListOf<WrappedServerMessage>()
    override fun send(wrappedServerMessage: WrappedServerMessage) {
        list.add(wrappedServerMessage)
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
