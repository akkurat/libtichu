package ch.taburett.tichu.game

import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.player.Round.AutoPlayer
import ch.taburett.tichu.game.player.SimpleBattle
import ch.taburett.tichu.game.player.StupidPlayer
import ch.taburett.tichu.game.protocol.PlayerMessage

class SimulationRound {

    val factories: Set<((PlayerMessage) -> Unit) -> AutoPlayer> = setOf(
        { StupidPlayer(it) },
//        { StrategicPlayer(it) },
//        { LessStupidPlayer(it) },
    )


    // todo: class per round
    fun start(_cardMap: MutableDeck? = null, lastMove: ImmutableTable?): SimpleBattle.Linfo {

        val cardMap = _cardMap ?: MutableDeck(Player.entries.zip(fulldeck.shuffled().chunked(14)).toMap())

        val serverQueue = ArrayDeque<WrappedServerMessage>()
        val playersQueue = ArrayDeque<WrappedPlayerMessage>()

        fun receiveServer(sm: WrappedServerMessage) {
            serverQueue.add(sm)
        }

        fun receivePlayer(pm: WrappedPlayerMessage) {
            playersQueue.add(pm)
        }

        val groupFactory = PlayerGroup.entries.associateWith { p -> factories.random() }

        // todo: external param
        val players = Player.entries.associateWith {
            groupFactory.getValue(it.playerGroup)({ m ->
                receivePlayer(WrappedPlayerMessage(it, m))
            })
        }

        val rp = RoundPlay(::receiveServer, cardMap, null, lastMove)


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
                if (starved > 100) {
                    rp.sendTableAndHandcards()
                    starved++
                }
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

}