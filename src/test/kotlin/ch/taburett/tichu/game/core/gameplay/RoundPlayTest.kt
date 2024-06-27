package ch.taburett.tichu.game.core.gameplay

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.EPlayer.*
import ch.taburett.tichu.game.communication.WrappedServerMessage
import ch.taburett.tichu.game.communication.Message.*
import ch.taburett.tichu.game.communication.createMove as move
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import ch.taburett.tichu.botplayer.play
import ch.taburett.tichu.game.core.common.IServerMessageSink
import kotlin.test.Test
import kotlin.test.assertTrue


class RoundPlayTest {

    val out = IServerMessageSink { println(it) }


    @Test
    fun testStartPlayer() {
        val map = EPlayer.entries.zip(fulldeck.chunked(14)).toMap()
        val round = GameRoundPlay(out, map, null, null)
        round.start()
        assertThat(round.mutableDeck.initialPlayer).isEqualTo(A1)

    }

    @Test
    fun wishNotAMoveAndFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )
        val messageSink = ArrayDeque<WrappedServerMessage>()
        val goneCards = (fulldeck-map.values.flatten()).filterIsInstance<PlayCard>()
        val deck = MutableDeck.createStarted(map, A2, goneCards)
        val round = GameRoundPlay(messageSink::add, deck, null, null)
        round.start()
        round.regularMove(A2, move(setOf(MAH), 5))

        assertThat(round.determineCurrentPlayer()).isEqualTo(B2)

    }

    @Test
    fun wishNotAMove() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH, S3),
            B2 to mutableListOf(S2),
        )
        val messageSink = ArrayDeque<WrappedServerMessage>()
        val goneCards = (fulldeck - map.values.flatten()).filterIsInstance<PlayCard>()
        val deck = MutableDeck.createStarted(map, A2,goneCards )
        val round = GameRoundPlay(messageSink::add, deck, null, null)

        round.start()
        round.regularMove(A2, move(setOf(MAH), 5))

        assertThat(round.determineCurrentPlayer()).isEqualTo(B2)
    }

    @Test
    fun testRejectWrongPlayer() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH, D2),
            B2 to mutableListOf(S2),
        )
        val goneCards = (fulldeck - map.values.flatten()).filterIsInstance<PlayCard>()
        val deck = MutableDeck.createStarted(map, A2,goneCards )
        val round = GameRoundPlay(out, deck, null, null)
        round.start()
        round.regularMove(A1, Move(setOf(J5)))

        assertThat(round.determineCurrentPlayer()).isEqualTo(A2)
    }

    @Test
    fun testTrickFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH, D2),
            B2 to mutableListOf(S2),
        )

        val goneCards = (fulldeck - map.values.flatten()).filterIsInstance<PlayCard>()
        val deck = MutableDeck.createStarted(map, A2,goneCards )
        val round = GameRoundPlay(out, deck, null, null)

        round.start()
        round.regularMove(A2, Move(setOf(MAH)))
        round.regularMove(B2, Move(setOf()))
        round.regularMove(A1, Move(setOf()))
        round.regularMove(B1, Move(setOf()))

        assertThat(round.determineCurrentPlayer()).isEqualTo(A2)
    }

    @Test
    fun playerFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )



        val goneCards = (fulldeck - map.values.flatten()).filterIsInstance<PlayCard>()
        val deck = MutableDeck.createStarted(map, A2,goneCards )
        val round = GameRoundPlay(out, deck, null, null)

        round.start()
        round.regularMove(A2, Move(setOf(MAH)))
        round.regularMove(B2, Move(setOf(S2)))
        round.regularMove(A1, Move(setOf()))
        round.regularMove(B1, Move(setOf()))

        assertThat(round.determineCurrentPlayer()).isEqualTo(A1)
    }

    @Test
    fun testFinished() {
        val map = mapOf(
            A1 to mutableListOf(PHX, J5),
            B1 to mutableListOf(DRG, S7),
            A2 to mutableListOf(MAH),
            B2 to mutableListOf(S2),
        )

        val goneCards = (fulldeck - map.values.flatten()).filterIsInstance<PlayCard>()
        val round = GameRoundPlay(out, MutableDeck.createStarted(map, A2, goneCards), null, null)

        round.start()
        round.regularMove(A2, Move(setOf(MAH)))
        round.regularMove(B2, Move(setOf(S2)))
        round.regularMove(A1, Move(setOf(J5)))
        round.regularMove(B1, Move(setOf(S7)))
        round.regularMove(A1, Move(setOf(PHX.asPlayCard(8))))

        assertAll(
            { assertThat(round.state == GameRoundPlay.State.FINISHED).isTrue() },
        )
    }

    @Test
    fun testEndingWithDog() {
        val map = mapOf(
            A1 to mutableListOf(),
            B1 to mutableListOf(),
            A2 to mutableListOf(DOG),
            B2 to mutableListOf(S2),
        )

        val goneCards = fulldeckAsPlayCards(2.5) - map.values.flatten()
        val deck = MutableDeck.createStarted(map, A2, goneCards)
        val round = GameRoundPlay(out, deck, null, null)

        round.receivePlayerMessage(A2.play(DOG))

        assertTrue { round.state == GameRoundPlay.State.FINISHED }

    }


}

