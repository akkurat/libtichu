package ch.taburett.tichu.game

import org.jetbrains.annotations.VisibleForTesting
import ru.nsk.kstatemachine.startBlocking
import ru.nsk.kstatemachine.stopBlocking
import java.util.concurrent.Executors

typealias Tricks = List<List<Played>>

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
    var playRound: MutableRound? = null

    fun start() {
        prepareRound = PrepareRound(com)
        prepareRound!!.start()
    }

    fun receiveUserMessage(msg: WrappedPlayerMessage) {
        receive(msg)
    }

    @Synchronized
    fun receive(wrappedPlayerMessage: WrappedPlayerMessage) {
        // todo: shouldn't switching happen inside state machine?
        val u = wrappedPlayerMessage.u
        when (val m = wrappedPlayerMessage.message) {
            is Ack, is Schupf -> prepareRound?.react(u, m)
            is Bomb -> TODO()
            is GiftDragon -> TODO()
            is Move -> playRound?.move(u, m)
            is Wish -> TODO()
            BigTichu -> TODO()
            Tichu -> TODO()
        }
        checkTransition()
    }

    private fun checkTransition() {
        if (prepareRound != null && prepareRound!!.isFinished) {
            playRound = MutableRound(com, prepareRound!!.cardMap)
            prepareRound = null
            playRound?.machine?.startBlocking()
        } else if (playRound != null && playRound!!.machine.isFinished) {
            val roundInfo = playRound!!.getRoundInfo()
            playLog.add(roundInfo)
            playerList.forEach {
                com.send(WrappedServerMessage(it, Points(roundInfo)))
            }
            playRound?.machine?.stopBlocking()
            playRound = null
            prepareRound = PrepareRound(com)
            prepareRound!!.start()
        }
    }
}

fun interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}


