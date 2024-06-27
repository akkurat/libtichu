package ch.taburett.tichu.game

import ch.taburett.tichu.game.core.Game
import ch.taburett.tichu.game.core.gameplay.RoundPlay
import ch.taburett.tichu.game.core.common.playerList
import ch.taburett.tichu.game.protocol.Message.*
import ch.taburett.tichu.game.protocol.WrappedPlayerMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import kotlin.test.Test

class GameTest {
    @Test
    fun testFinished() {
        runBlocking {
            val game = Game(out)
            game.start()
            for (p in playerList) {
                game.receive(WrappedPlayerMessage(p, Announce.BigTichu()))
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedPlayerMessage(p, Ack.TichuBeforeSchupf()))
            }
            delay(10)
            for (p in playerList) {
                randomShupf(game.prepareRound!!, p)
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedPlayerMessage(p, Ack.SchupfcardReceived()))
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedPlayerMessage(p, Ack.TichuBeforePlay()))
            }
            assertAll(
                { assertThat(game.prepareRound).isNull() },
                { assertThat(game.roundPlay!!.state == RoundPlay.State.RUNNING).isTrue() },
            )
        }
    }
}