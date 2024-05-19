package tichu

import ch.taburett.tichu.game.Ack
import ch.taburett.tichu.game.Ack.BigTichu
import ch.taburett.tichu.game.Ack.TichuBeforeSchupf
import ch.taburett.tichu.game.Game
import ch.taburett.tichu.game.WrappedUserMessage
import ch.taburett.tichu.game.playerList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertTrue

class GameTest {
    @Test
    fun testFinished() {
        runBlocking {
            val game = Game(out)
            game.start()
            for (p in playerList) {
                game.receive(WrappedUserMessage(p, BigTichu()))
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedUserMessage(p, TichuBeforeSchupf()))
            }
            delay(10)
            for (p in playerList) {
                randomShupf(game.p, p)
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedUserMessage(p, Ack.SchupfcardReceived()))
            }
            delay(10)
            for (p in playerList) {
                game.receive(WrappedUserMessage(p, Ack.TichuBeforePlay()))
            }
            assertAll(
                { assertTrue { game.p.machine.isFinished } },
                { assertTrue { game.round.machine.isRunning } },
            )
        }
    }
}