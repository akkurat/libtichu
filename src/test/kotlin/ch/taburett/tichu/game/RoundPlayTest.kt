package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.MutableDeck
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.RoundPlay
import ch.taburett.tichu.game.Player.*
import ch.taburett.tichu.game.WrappedServerMessage
import ch.taburett.tichu.game.protocol.Message.*
import ch.taburett.tichu.game.protocol.createMove as move
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import ch.taburett.tichu.player.play
import kotlin.test.Test
import kotlin.test.assertTrue


class RoundPlayTest {


    @Test
    fun testStartPlayer() {
        val map = Player.entries.zip(fulldeck.chunked(14)).toMap()
        val round = RoundPlay(out, map, null, null)
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
        val round = RoundPlay(messageSink::add, deck, null, null)
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
        val round = RoundPlay(messageSink::add, deck, null, null)

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
        val round = RoundPlay(out, deck, null, null)
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
        val round = RoundPlay(out, deck, null, null)

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
        val round = RoundPlay(out, deck, null, null)

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
        val round = RoundPlay(out, MutableDeck.createStarted(map, A2, goneCards), null, null)

        round.start()
        round.regularMove(A2, Move(setOf(MAH)))
        round.regularMove(B2, Move(setOf(S2)))
        round.regularMove(A1, Move(setOf(J5)))
        round.regularMove(B1, Move(setOf(S7)))
        round.regularMove(A1, Move(setOf(PHX.asPlayCard(8))))

        assertAll(
            { assertThat(round.state == RoundPlay.State.FINISHED).isTrue() },
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
        val round = RoundPlay(out, deck, null, null)

        round.receivePlayerMessage(A2.play(DOG))

        assertTrue { round.state == RoundPlay.State.FINISHED }

    }


}

