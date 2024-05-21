package tichu

import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.Ack.BigTichu
import ch.taburett.tichu.game.Ack.TichuBeforeSchupf
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
                game.receive(WrappedPlayerMessage(p, BigTichu()))
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedPlayerMessage(p, TichuBeforeSchupf()))
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
                { assertThat(game.playRound!!.state == MutableRound.State.RUNNING).isTrue() },
            )
        }
    }
}