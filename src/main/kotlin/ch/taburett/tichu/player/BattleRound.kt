@file:OptIn(ExperimentalStdlibApi::class)

package ch.taburett.tichu.player

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.core.common.ETichu
import ch.taburett.tichu.game.core.common.Player
import ch.taburett.tichu.game.core.common.PlayerGroup
import ch.taburett.tichu.game.core.common.TichuGameStage
import ch.taburett.tichu.game.core.gameplay.RoundPlay
import ch.taburett.tichu.game.core.preparation.PrepareRound
import ch.taburett.tichu.player.BattleRound.AutoPlayer
import ch.taburett.tichu.player.SimpleBattle.BattleResult
import ch.taburett.tichu.game.protocol.Message
import ch.taburett.tichu.game.protocol.Message.ServerMessage
import ch.taburett.tichu.game.protocol.WrappedPlayerMessage
import ch.taburett.tichu.game.protocol.WrappedServerMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach


private const val N = 10000

fun main() {
    val simpleBattle = SimpleBattle(N)
    val roundlog: MutableList<SimpleBattle.Linfo> = ArrayList()
    runBlocking {
        simpleBattle.start().consumeEach { info ->
            val roundName = info.roundPlay.name
            val out =
                if (info is BattleResult) "${mapPoints(info).toMap()} ${info.roundPlay.getRoundInfo().tichuPointsPerPlayer}" else "starved"
            println("$roundName $out")
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
        val player = s.roundPlay.determineCurrentPlayer()
        println(s.players.getValue(player))
    }
}

private fun printAvg(roundlog: MutableList<SimpleBattle.Linfo>) {
    val finishedRounds = roundlog.filterIsInstance<BattleResult>()
    val rb = finishedRounds
        .flatMap(::mapPoints)
        .groupBy({ it.first }, { it.second })
    println(rb.mapValues { it.value.average() })

    val bigTichuCounts = mutableMapOf<String, Int>()
    val smallTichuCounts = mutableMapOf<String, Int>()
    val bigTichuWins = mutableMapOf<String, Int>()
    val smallTichuWins = mutableMapOf<String, Int>()

    for (round in finishedRounds) {
        for (tm in round.roundPlay.tichuMap) {
            val type = round.players.getValue(tm.key).type
            if (tm.value == ETichu.BIG) {
                bigTichuCounts[type] = bigTichuCounts.getOrPut(type) { 0 } + 1
                if (round.roundPlay.getRoundInfo().orderOfWinning.first() == tm.key) {
                    bigTichuWins[type] = bigTichuWins.getOrPut(type) { 0 } + 1
                }
            } else if (tm.value == ETichu.SMALL) {
                smallTichuCounts[type] = smallTichuCounts.getOrPut(type) { 0 } + 1
                if (round.roundPlay.getRoundInfo().orderOfWinning.first() == tm.key) {
                    smallTichuWins[type] = smallTichuWins.getOrPut(type) { 0 } + 1
                }
            }
        }
    }

    // sometimes a map and a for loop are way easieer than mapping the shit
//    val s = roundlog.filterIsInstance<BattleResult>()
//        .flatMap { br ->
//            val rp = br.roundPlay
//            rp.tichuMap.mapValues {
//                it.value to rp.getRoundInfo().tichuPointsPerPlayer.getValue(it.key)
//            }.mapKeys { br.players.getValue(it.key).type }
//                .toList()
//
//        }
//
//    val grouped = s.groupBy { it.first }.mapValues { it.value.groupBy { it.second } }
////        .groupBy({ it.first }, { it.second })
//    println(grouped.mapValues { it.value.values.size })
    val numRounds = finishedRounds.count()

    val big = bigTichuCounts.mapValues { it.value.toDouble() / numRounds }
    val small = smallTichuCounts.mapValues { it.value.toDouble() / numRounds }

    val bigwins = bigTichuWins.mapValues { it.value.toDouble() / bigTichuCounts.getValue(it.key) }
    val smallwins = smallTichuWins.mapValues { it.value.toDouble() / smallTichuCounts.getValue(it.key) }

    println("small $small wins $smallwins big $big wins $bigwins")


}


private fun mapPoints(s: BattleResult): List<Pair<Grr, Int>> {
    val ri = s.roundPlay.getRoundInfo()
    val lu = s.players::getValue
    if (ri.tichuMap.any { it.value == ETichu.BIG }) {
        ri.tichuPoints
    }
    val battleResult = ri.pointsPerPlayer
        .map { (p, v) ->
            Grr(setOf(lu(p).type, lu(p.partner).type), setOf(lu(p.re).type, lu(p.li).type)) to v
        }
    return battleResult
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
    data class BattleResult(override val roundPlay: RoundPlay, override val players: Map<Player, AutoPlayer>) :
        Linfo {
        override val finished: Boolean = true
    }

    data class PrepareInterrupted(
        override val roundPlay: PrepareRound,
        override val players: Map<Player, AutoPlayer>,
    ) : Linfo {
        override val finished: Boolean = false
    }

    data class BattleInterrupted(
        override val roundPlay: RoundPlay,
        override val players: Map<Player, AutoPlayer>,
    ) : Linfo {
        override val finished: Boolean = false
    }

    interface Linfo {
        val finished: Boolean
        val roundPlay: TichuGameStage
        val players: Map<Player, AutoPlayer>
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun start(): Channel<Linfo> {
        val channel = Channel<Linfo>()
        val out = (1..n).map {
            // limit is useful for debugging
            GlobalScope.async(Dispatchers.Default) {
                val battleRound = BattleRound(it.toHexString(hexhex))
                val info = battleRound.start()
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

class BattleRound(val name: String) {

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

        val factories: Set<((Message.PlayerMessage) -> Unit) -> AutoPlayer> =
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

        val prep = PrepareRound(::receiveServer, name)
        prep.start()
        var starved = 0
        while (!prep.isFinished) {
            val sm = serverQueue.removeFirstOrNull()
            if (sm != null) {
                players.getValue(sm.u).receiveMessage(sm.message, sm.u)
            }
            val pm = playersQueue.removeFirstOrNull()
            if (pm != null) {
                starved = 0
                prep.receivePlayerMessage(pm)
            } else {
                starved++
            }
            if (starved > 200) {
                return SimpleBattle.PrepareInterrupted(prep, players)
            }

        }

        val rp = RoundPlay(::receiveServer, cardMap, prep.preparationInfo, null, name)
        rp.start()

        starved = 0

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

val hexhex = HexFormat {
    this.upperCase = true
    this.number.removeLeadingZeros = true
}