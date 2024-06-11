package ch.taburett.tichu.game.player

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.player.SimpleBattle.BattleResult
import ch.taburett.tichu.game.protocol.PlayerMessage
import ch.taburett.tichu.game.protocol.ServerMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach


private const val N = 100

fun main() {
    val simpleBattle = SimpleBattle(N)
    val roundlog: MutableList<SimpleBattle.Linfo> = ArrayList()
    runBlocking {
        simpleBattle.start().consumeEach { info ->
            if (info is BattleResult)
                println(mapResult(info))
            else println("Starved")
            roundlog.add(info)
            printAvg(roundlog)
        }
    }

    println(
        "finished: ${roundlog.filterIsInstance<BattleResult>().size}\n" +
                "starved: ${roundlog.filterIsInstance<SimpleBattle.BattleInterrupted>().size}\n"
//                "error: ${simpleBattle.errorlog.size}"
    )

    printAvg(roundlog)

    for (s in roundlog.filterIsInstance<SimpleBattle.BattleInterrupted>()) {
        val player = s.roundInfo.determineCurrentPlayer()
        println(s.players.getValue(player))
    }

}

private fun printAvg(roundlog: MutableList<SimpleBattle.Linfo>) {
    val rb = roundlog.filterIsInstance<BattleResult>()
        .flatMap(::mapResult)
        .groupBy({ it.first }, { it.second })
    println(rb.mapValues { it.value.average() })
}

private fun mapResult(s: BattleResult): List<Pair<Grr, Int>> {
    val ri = s.roundInfo.getRoundInfo()
    val lu = s.players::getValue
    val shit = ri.pointsPerPlayer
        .map { (p, v) ->
            Grr(setOf(lu(p).type, lu(p.partner).type), setOf(lu(p.re).type, lu(p.li).type)) to v
        }
    return shit
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
        return "${myteam}->${otherPlayers}"
    }

}

/**
 * Single rounds not complete games
 * advanced players might increase risk
 * when close to 1000
 */
class SimpleBattle(private val n: Int = 5000) {
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

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start(): Channel<Linfo> {
        val channel = Channel<Linfo>()
        val out = (1..n).map {
            GlobalScope.async(Dispatchers.Default) {
                val round = Round()
                val info = round.start()
                channel.send(info)
            }
        }
        GlobalScope.launch {
            out.awaitAll()
            channel.close()
        }
        return channel
    }
}

class Round {


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

        val factories: Set<((PlayerMessage) -> Unit) -> AutoPlayer> =
            setOf(
                { StupidPlayer(it) },
                { StrategicPlayer(it) },
//            { LessStupidPlayer(it) },
            )
        val groupFactory = PlayerGroup.entries.zip(factories).toMap()

        val players = Player.entries.associateWith {
            groupFactory.getValue(it.playerGroup)({ m ->
                receivePlayer(WrappedPlayerMessage(it, m))
            })
        }

//        val players =
//            Player.entries.associateWith { p -> factories.random()({ m -> receivePlayer(WrappedPlayerMessage(p, m)) }) }

        val rp = RoundPlay(::receiveServer, cardMap, null, null)

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
        return BattleResult(rp, players)

    }

    interface AutoPlayer {
        fun receiveMessage(message: ServerMessage, player: Player)
        val type: String
    }
}