package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.protocol.PlayerMessage
import ch.taburett.tichu.game.protocol.ServerMessage


fun main() {

    val battle = Battle(1000)
    battle.start()
    println(battle.roundlog.joinToString("\n"))
    if (battle.errorlog.isNotEmpty()) {
        error(battle.errorlog.joinToString("\n", "\n"))
    }
}

class Battle(val rounds: Int = 5000) {


    val factories: Set<((PlayerMessage) -> Unit) -> AutoPlayer> =
        setOf(
//            { StupidPlayer(it) },
//            { StrategicPlayer(it) },
            { LessStupidPlayer(it) },
        )

    private lateinit var players: Map<Player, AutoPlayer>

    val errorlog = mutableListOf<String>()
    val roundlog = mutableListOf<String>()

    fun start() {
        for (i in 1..rounds) {
            try {
                println(round())
                roundlog.add("finished")
            } catch (e: Exception) {
                errorlog.add(e.toString())
            }
        }
    }

    // todo: class per round
    fun round(): RoundInfo {

        val cardMap = Player.entries.zip(fulldeck.shuffled().chunked(14)).toMap()

        val serverQueue = ArrayDeque<WrappedServerMessage>()
        val playersQueue = ArrayDeque<WrappedPlayerMessage>()

        fun receiveServer(sm: WrappedServerMessage) {
            // todo: logging facade, e.g. https://github.com/oshai/kotlin-logging/wiki
            println(sm)
            serverQueue.add(sm)
        }

        fun receivePlayer(pm: WrappedPlayerMessage) {
            println(pm)
            playersQueue.add(pm)
        }

        val players =
            Player.entries.associateWith { p -> factories.random()({ m -> receivePlayer(WrappedPlayerMessage(p, m)) }) }

        val rp = RoundPlay(::receiveServer, cardMap, null)


        rp.start()

        var starved = 0
        while (rp.state == RoundPlay.State.RUNNING ) {
            val sm = serverQueue.removeFirstOrNull()
            if (sm != null) {
                players.getValue(sm.u).receiveMessage(sm.message, sm.u)
            }
            val pm = playersQueue.removeFirstOrNull()
            if (pm != null) {
                starved = 0
                rp.receivePlayerMessage(pm)
            } else {
                starved++
            }
            if(starved > 100) {
                rp.sendTableAndHandcards()
            }
            if(starved > 200 ) {
                break
            }
        }

        return rp.getRoundInfo()


    }

    interface AutoPlayer {
        fun receiveMessage(message: ServerMessage, player: Player)
    }
}