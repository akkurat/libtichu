package ch.taburett.tichu.game


// TODO: more lightweight wrapper for com
class Game(val com: Out) {


    lateinit var currentRound: MutableRound

    fun start() {
        currentRound = MutableRound(com)
//        currentRound.start()
    }

    fun receiveUserMessage(msg: WrappedUserMessage) {
        currentRound.receive(msg)
    }


}

interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}


