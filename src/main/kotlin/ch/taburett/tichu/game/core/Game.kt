package ch.taburett.tichu.game.core

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.core.common.playerList
import ch.taburett.tichu.game.core.gameplay.GameRoundPlay
import ch.taburett.tichu.game.core.preparation.GameRoundPrepare
import ch.taburett.tichu.game.gamelog.RoundInfo
import ch.taburett.tichu.game.communication.*
import ch.taburett.tichu.game.core.common.IServerMessageSink
import java.util.concurrent.Executors


class Game(serverMessageSink: IServerMessageSink) {
    val executor = Executors.newCachedThreadPool()

    // todo: use async and not real threads
    val com = IServerMessageSink { msg ->
        executor.execute {
            try {
                serverMessageSink.send(msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val playLog = mutableListOf<RoundInfo>()

    var prepareRound: GameRoundPrepare? = null

    var roundPlay: GameRoundPlay? = null

    // todo: deserialize a stored game
    @JvmOverloads
    fun start(jumpPrepartion: Boolean = false) {
        if (!jumpPrepartion) {
            prepareRound = GameRoundPrepare(com)
            prepareRound!!.start()
        } else {
            // no shupf for quick testing
            var rigged = setOf(DRG, DOG, PHX, MAH, D2, S2, P2, J2)
//            val paeckli = ((fulldeck - rigged).shuffled() + rigged).chunked(14)
            val paeckli = fulldeck.shuffled().chunked(14)
            val cardmap = playerList.zip(paeckli).toMap()

            roundPlay = GameRoundPlay(com, cardmap, null, null)
            roundPlay!!.start()
        }

    }

    fun receiveUserMessage(msg: WrappedPlayerMessage) {
        receive(msg)
    }

    fun resendStatus() {
        roundPlay?.sendTableAndHandcards();
    }

    // todo: this is probably still necessary
    // or look up how to use a "scheduler" thread for communication order and a
    @Synchronized
    fun receive(wrappedPlayerMessage: WrappedPlayerMessage) {
        // todo: shouldn't switching happen inside state machine?
        prepareRound?.receivePlayerMessage(wrappedPlayerMessage)
        roundPlay?.receivePlayerMessage(wrappedPlayerMessage)
        checkTransition()
    }

    // TODO: 1000 points, maybe configurable
    private fun checkTransition() {
        if (prepareRound != null && prepareRound!!.isFinished) {
            roundPlay = GameRoundPlay(com, prepareRound!!.cardMap, prepareRound!!.preparationInfo, null)
            prepareRound = null
            roundPlay!!.start()
        } else if (roundPlay != null && roundPlay!!.state == GameRoundPlay.State.FINISHED) {
            val roundInfo = roundPlay!!.getRoundInfo()
            playLog.add(roundInfo)
            playerList.forEach {
                com.send(WrappedServerMessage(it, Message.Points(roundInfo)))
            }
            roundPlay = null
            prepareRound = GameRoundPrepare(com)
            prepareRound!!.start()
        }
    }
}

