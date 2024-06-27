package ch.taburett.tichu.game.core

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.core.common.ETichu
import ch.taburett.tichu.game.core.common.Player
import ch.taburett.tichu.game.core.common.playerList
import ch.taburett.tichu.game.core.gameplay.RoundPlay
import ch.taburett.tichu.game.core.preparation.PrepareRound
import ch.taburett.tichu.game.gamelog.RoundInfo
import ch.taburett.tichu.game.protocol.*
import java.util.concurrent.Executors


typealias PlayerETichuMutableMap = MutableMap<Player, ETichu>

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

    var prepareRound: PrepareRound? = null

    var roundPlay: RoundPlay? = null

    // todo: deserialize a stored game
    @JvmOverloads
    fun start(jumpPrepartion: Boolean = false) {
        if (!jumpPrepartion) {
            prepareRound = PrepareRound(com)
            prepareRound!!.start()
        } else {
            // no shupf for quick testing
            var rigged = setOf(DRG, DOG, PHX, MAH, D2, S2, P2, J2)
//            val paeckli = ((fulldeck - rigged).shuffled() + rigged).chunked(14)
            val paeckli = fulldeck.shuffled().chunked(14)
            val cardmap = playerList.zip(paeckli).toMap()

            roundPlay = RoundPlay(com, cardmap, null, null)
            roundPlay!!.start()
        }

    }

    fun receiveUserMessage(msg: WrappedPlayerMessage) {
        receive(msg)
    }

    fun resendStatus() {
        roundPlay?.sendTableAndHandcards();
    }

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
            roundPlay = RoundPlay(com, prepareRound!!.cardMap, prepareRound!!.preparationInfo, null)
            prepareRound = null
            roundPlay!!.start()
        } else if (roundPlay != null && roundPlay!!.state == RoundPlay.State.FINISHED) {
            val roundInfo = roundPlay!!.getRoundInfo()
            playLog.add(roundInfo)
            playerList.forEach {
                com.send(WrappedServerMessage(it, Message.Points(roundInfo)))
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