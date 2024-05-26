package ch.taburett.tichu.game

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.protocol.*
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.Executors

typealias Tricks = List<Trick>


class Game(com: Out) {
    val executor = Executors.newCachedThreadPool()

    val com = Out { msg ->
        executor.execute {
            try {
                com.send(msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val playLog = mutableListOf<RoundInfo>()

    @VisibleForTesting
    var prepareRound: PrepareRound? = null

    @VisibleForTesting
    var roundPlay: RoundPlay? = null

    // todo: deserialize a stored game
    @JvmOverloads
    fun start(jumpPrepartion: Boolean = false) {
        if (!jumpPrepartion) {
            prepareRound = PrepareRound(com)
            prepareRound!!.start()
        } else {
            // no shupf for quick testing
            val cardmap = playerList.zip(fulldeck.shuffled().chunked(14)).toMap()
            roundPlay = RoundPlay(com, cardmap, null)
            roundPlay!!.start()
        }

    }

    fun receiveUserMessage(msg: WrappedPlayerMessage) {
        receive(msg)
    }

    internal fun resendStatus() {
        if (roundPlay != null) {
            roundPlay!!.sendTableAndHandcards(roundPlay!!.table.currentPlayer);
        }
    }

    @Synchronized
    fun receive(wrappedPlayerMessage: WrappedPlayerMessage) {
        // todo: shouldn't switching happen inside state machine?
        prepareRound?.receive( wrappedPlayerMessage)
        roundPlay?.receive( wrappedPlayerMessage)
        checkTransition()
    }

    private fun checkTransition() {
        if (prepareRound != null && prepareRound!!.isFinished) {
            roundPlay = RoundPlay(com, prepareRound!!.cardMap, prepareRound!!.preparationInfo)
            prepareRound = null
            roundPlay!!.start()
        } else if (roundPlay != null && roundPlay!!.state == RoundPlay.State.FINISHED) {
            val roundInfo = roundPlay!!.getRoundInfo()
            playLog.add(roundInfo)
            playerList.forEach {
                com.send(WrappedServerMessage(it, Points(roundInfo)))
            }
            roundPlay = null
            prepareRound = PrepareRound(com)
            prepareRound!!.start()
        }
    }
}

fun interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}

enum class TichuType {
   BIG, SMALL
}