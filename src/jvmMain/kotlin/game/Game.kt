package game

import ru.nsk.kstatemachine.startBlocking


// TODO: more lightweight wrapper for com
class Game(val com: Out) {


    lateinit var currentRound: MutableRound

    fun start() {
        currentRound = MutableRound(com,false)
        currentRound.machine.startBlocking()
    }

    fun receiveUserMessage(msg: WrappedUserMessage) {
        currentRound.receive(msg)
    }


}

fun interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}


