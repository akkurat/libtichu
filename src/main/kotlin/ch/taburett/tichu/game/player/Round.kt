package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.protocol.PlayerMessage
import ch.taburett.tichu.game.protocol.ServerMessage
import kotlinx.coroutines.*


fun main() {
    val simpleBattle = SimpleBattle(100)
    val roundlog: List<SimpleBattle.Linfo>
    runBlocking {
        roundlog = simpleBattle.start()
    }

    println(
        "finished: ${roundlog.filterIsInstance<SimpleBattle.BattleResult>().size}\n" +
                "starved: ${roundlog.filterIsInstance<SimpleBattle.BattleInterrupted>().size}\n"
//                "error: ${simpleBattle.errorlog.size}"
    )


    val resultsbytype = roundlog.filterIsInstance<SimpleBattle.BattleResult>()
        .flatMap { s ->
            val ri = s.roundInfo.getRoundInfo()
            ri.pointsPerPlayer.map { s.players.getValue(it.key).type to it.value }
        }
        .groupBy({ it.first }, { it.second })
//    println(resultsbytype)
    println(resultsbytype.mapValues { it.value.average() })

    val rb = roundlog.filterIsInstance<SimpleBattle.BattleResult>()
        .flatMap { s ->
            val ri = s.roundInfo.getRoundInfo()
            val lu = s.players::getValue
            val shit = ri.pointsPerPlayer.map { (p, v) ->
                Grr(setOf(lu(p).type, lu(p.partner).type), setOf(lu(p.re).type, lu(p.li).type)) to v
            }
            shit
        }
        .groupBy({ it.first }, { it.second })
//    println(rb)
    println(rb.mapValues { it.value.average() })

    for (s in roundlog.filterIsInstance<SimpleBattle.BattleInterrupted>()) {
        val player = s.roundInfo.determineCurrentPlayer()
        println(s.players.getValue(player))
    }

}

class Grr(val myteam: Set<String>, val otherPlayers: Set<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Grr) return false

        if (myteam != other.myteam) return false
        if (otherPlayers != other.otherPlayers) return false

        return true

    }

    override fun hashCode(): Int {
        var result = myteam.hashCode()
        result = 31 * result + otherPlayers.hashCode()
        return result
    }

    override fun toString(): String {
        return "${myteam.map { it.substring(0..1) }}->${otherPlayers.map { it.substring(0..1) }}"
    }

}

/**
 * Single rounds not complete games
 * advanced players might increase risk
 * when close to 1000
 */
class SimpleBattle(val N: Int = 5000) {
    data class BattleResult(override val roundInfo: RoundPlay, override val players: Map<Player, Round.AutoPlayer>) :
        Linfo {
        override val finished: Boolean = true
    }

    data class BattleInterrupted(
        override val roundInfo: RoundPlay,
        override val players: Map<Player, Round.AutoPlayer>,
    ) : Linfo {
        override val finished: Boolean = false
    }

    interface Linfo {
        val finished: Boolean
        val roundInfo: RoundPlay
        val players: Map<Player, Round.AutoPlayer>
    }

    suspend fun start(): List<Linfo> {
        val roundlog = coroutineScope {
            (1..N).map {
                async(Dispatchers.Default) {
                    //                println(round())
                    print("${it}s${Thread.currentThread().threadId()} ")
                    val round = Round()
                    val info = round.start()
                    print("${it}f${Thread.currentThread().threadId()} ")
                    info
                }
            }.awaitAll()
        }
        return roundlog
    }
}

class Round {

    val factories: Set<((PlayerMessage) -> Unit) -> AutoPlayer> =
        setOf(
            { StupidPlayer(it) },
            { StrategicPlayer(it) },
            { LessStupidPlayer(it) },
        )

    private lateinit var players: Map<Player, AutoPlayer>

    // todo: class per round
    fun start(): SimpleBattle.Linfo {

        val cardMap = Player.entries.zip(fulldeck.shuffled().chunked(14)).toMap()

        val serverQueue = ArrayDeque<WrappedServerMessage>()
        val playersQueue = ArrayDeque<WrappedPlayerMessage>()

        fun receiveServer(sm: WrappedServerMessage) {
            // todo: logging facade, e.g. https://github.com/oshai/kotlin-logging/wiki
//            println(sm)
            serverQueue.add(sm)
        }

        fun receivePlayer(pm: WrappedPlayerMessage) {
//            println(pm)
            playersQueue.add(pm)
        }


        val groupFactory = PlayerGroup.entries.associateWith { p -> factories.random() }


        val players = Player.entries.associateWith {
            groupFactory.getValue(it.playerGroup)({ m ->
                receivePlayer(WrappedPlayerMessage(it, m))
            })
        }

//        val players =
//            Player.entries.associateWith { p -> factories.random()({ m -> receivePlayer(WrappedPlayerMessage(p, m)) }) }

        val rp = RoundPlay(::receiveServer, cardMap, null)


        rp.start()

        var starved = 0

        while (rp.state == RoundPlay.State.RUNNING) {
            try {
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
//            if (starved > 100) {
//                rp.sendTableAndHandcards()
//                starved++
//            }
                if (starved > 200) {
                    break
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
        if (starved > 200) {
            return SimpleBattle.BattleInterrupted(rp, players)
        }
        return SimpleBattle.BattleResult(rp, players)

    }

    interface AutoPlayer {
        fun receiveMessage(message: ServerMessage, player: Player)
        val type: String
    }
}