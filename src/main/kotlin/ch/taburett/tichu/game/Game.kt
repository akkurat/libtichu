package ch.taburett.tichu.game

import ru.nsk.kstatemachine.*

class Game(val com: Out) {


    lateinit var p: PrepareRound
    lateinit var round : MutableRound

    fun start() {

//        createStdLibStateMachine {
//            val start = addInitialState( p.machine)
//            addState( round.machine ) {
//                transition<FinishedEvent> {
//                    targetState = start
//                }
//            }
//        }
        p = PrepareRound(com)
        p.machine.startBlocking()
        round = MutableRound(com, mapOf())
        round.machine.startBlocking()

    }

    fun receiveUserMessage(msg: WrappedUserMessage) {
        receive(msg)
    }
    fun receive(wrappedUserMessage: WrappedUserMessage) {
        // todo: shouldn't switching happen inside state machine?
        val u = wrappedUserMessage.u
        when (val m = wrappedUserMessage.message) {
            is Ack -> p.ack(u, m)
            is Schupf -> p.schupf(mapSchupfEvent(u, m))
            is Bomb -> TODO()
            is GiftDragon -> TODO()
            is Move -> round.move(u, m)
            is Wish -> TODO()
            BigTichu -> TODO()
            Tichu -> TODO()
        }
//        machine.processEventBlocking(wrappedUserMessage)
    }

    private fun mapSchupfEvent(u: Player, schupf: Schupf): SchupfEvent {
        val cards = mapOf(
            u.partner() to schupf.partner,
            u.li() to schupf.li,
            u.re() to schupf.re,
        )
        return SchupfEvent(u, cards)

    }

}

fun interface Out {
    fun send(wrappedServerMessage: WrappedServerMessage)
}


