package ch.taburett.tichu.game

import ru.nsk.kstatemachine.startBlocking

class Game(val com: Out) {


    enum class GameState { PREPARE, GAME, END }

    lateinit var p: PrepareRound
    lateinit var round: MutableRound


    lateinit var state: GameState

    fun start() {
        state = GameState.PREPARE
        p = PrepareRound(com)
        p.start()
    }

    fun receiveUserMessage(msg: WrappedPlayerMessage) {
        receive(msg)
    }

    fun receive(wrappedPlayerMessage: WrappedPlayerMessage) {
        // todo: shouldn't switching happen inside state machine?
        val u = wrappedPlayerMessage.u
        when (val m = wrappedPlayerMessage.message) {
            is Ack, is Schupf -> p.react(u, m)
            is Bomb -> TODO()
            is GiftDragon -> TODO()
            is Move -> round.move(u, m)
            is Wish -> TODO()
            BigTichu -> TODO()
            Tichu -> TODO()
        }
        checkTransition()


    }

    private fun checkTransition() {
        if (state == GameState.PREPARE) {
            if (p.isFinished) {
                state = GameState.GAME
                round = MutableRound(com, p.cardMap)
                round.machine.startBlocking()
            }
        } else if (state == GameState.GAME) {
            if (round.machine.isFinished) {
                state = GameState.PREPARE
                p = PrepareRound(com)
            }
        }

    }


}

fun interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}


