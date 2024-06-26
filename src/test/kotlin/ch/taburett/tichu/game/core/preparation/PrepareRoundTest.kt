package ch.taburett.tichu.game.core.preparation

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.core.common.EPlayer.*
import ch.taburett.tichu.game.core.preparation.GameRoundPrepare.preGame
import ch.taburett.tichu.game.core.preparation.GameRoundPrepare.schupfed
import ch.taburett.tichu.game.core.common.IServerMessageSink
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.preparation.GameRoundPrepare
import ch.taburett.tichu.game.core.common.playerList
import ch.taburett.tichu.game.communication.Message
import ch.taburett.tichu.game.communication.Message.Ack
import ch.taburett.tichu.game.communication.Message.Schupf
import ch.taburett.tichu.game.communication.WrappedServerMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.function.Executable
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test


class PrepareRoundTest {

    val out = IServerMessageSink { println(it) }

    @Test
    fun test8Cards() {

        val round = GameRoundPrepare(out)
        round.start()

        val cardsCheck = round.cardMap.values
            .map { c -> Executable { assertEquals(8, c.size) } }
            .toList()

        assertAll(
            { println(round.currentPreparationState) },
            { assertAll(cardsCheck) },
        )
    }

    @Test
    fun transtionToAllCards() {
        val round = GameRoundPrepare(out)
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
        val round = GameRoundPrepare(out)
        round.start()
        playerList.forEach { a -> round.react(a, Ack.BigTichu()) }
        playerList.forEach { a -> round.react(a, Ack.TichuBeforeSchupf()) }
        // todo: where to check validity of schupf payload?
        // assumming it's only occuring deliberately
        //
        randomShupf(round, A1)
        val state1 = round.currentPreparationState
        listOf(A2, B1, B2).forEach { p -> randomShupf(round, p) }

        val state2 = round.currentPreparationState

        playerList.forEach { p -> round.react(p, Ack.SchupfcardReceived()) }
        val state3 = round.currentPreparationState

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
        val round = GameRoundPrepare(outCollector)
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

class OutCollector : IServerMessageSink {
    val list = mutableListOf<WrappedServerMessage>()
    override fun send(wrappedServerMessage: WrappedServerMessage) {
        list.add(wrappedServerMessage)
    }

}

fun randomShupf(round: GameRoundPrepare, player: EPlayer) {
    val input = round.cardMap.get(player)
    if (input != null) {
        val toSchupf = input.take(3)
        round.react(player, Schupf(toSchupf[0], toSchupf[1], toSchupf[2]))
    } else {
        throw NullPointerException("")
    }
}
