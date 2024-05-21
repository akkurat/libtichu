package ch.taburett.tichu.game

import ch.taburett.tichu.cards.fulldeck
import org.jetbrains.annotations.VisibleForTesting
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

    // todo: deserialize a stored game
    fun start() {
//        prepareRound = PrepareRound(com)
//        prepareRound!!.start()
        // no shupf for quick testing
        val cardmap = playerList.zip(fulldeck.shuffled().chunked(14)).toMap()
        playRound = MutableRound(com, cardmap)
        playRound!!.start()

    }

    fun receiveUserMessage(msg: WrappedPlayerMessage) {
        receive(msg)
    }

    internal fun resendStatus() {
        if(playRound != null ) {
            playRound!!.sendTableAndHandcards(playRound!!.currentPlayer);
        }
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
            playRound!!.start()
        } else if (playRound != null && playRound!!.state == MutableRound.State.FINISHED) {
            val roundInfo = playRound!!.getRoundInfo()
            playLog.add(roundInfo)
            playerList.forEach {
                com.send(WrappedServerMessage(it, Points(roundInfo)))
            }
            playRound = null
            prepareRound = PrepareRound(com)
            prepareRound!!.start()
        }
    }
}

fun interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}


