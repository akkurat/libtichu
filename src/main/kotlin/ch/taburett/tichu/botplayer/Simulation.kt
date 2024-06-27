@file:OptIn(ExperimentalStdlibApi::class)

package ch.taburett.tichu.botplayer

import ch.taburett.tichu.game.core.common.IDeck
import ch.taburett.tichu.game.core.gameplay.MutableDeck
import ch.taburett.tichu.game.gamelog.MutableTricks
import ch.taburett.tichu.game.core.gameplay.GameRoundPlay
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.EPlayerGroup
import ch.taburett.tichu.botplayer.BattleRound.AutoPlayer
import ch.taburett.tichu.game.communication.Message.PlayerMessage
import ch.taburett.tichu.game.communication.WrappedMessage
import ch.taburett.tichu.game.communication.WrappedPlayerMessage
import ch.taburett.tichu.game.communication.WrappedServerMessage

class SimulationRound(

    _deck: IDeck? = null,
    soFareMoves: MutableTricks,
    _playersFactory: ((a: EPlayer, b: (PlayerMessage) -> Unit) -> AutoPlayer)? = null,
) {

    // todo: class per round

    val deck = _deck?.let { MutableDeck.copy(it) } ?: MutableDeck.createInitial()

    val serverQueue = ArrayDeque<WrappedServerMessage>()
    val playersQueue = ArrayDeque<WrappedPlayerMessage>()
    val history = mutableListOf<WrappedMessage>()

    fun receiveServer(sm: WrappedServerMessage) {
        serverQueue.add(sm)
    }

    fun receivePlayer(pm: WrappedPlayerMessage) {
        playersQueue.add(pm)
    }

    val groupFactory = EPlayerGroup.entries.associateWith { p -> }

    val playersFactory = _playersFactory ?: { a, com -> StupidPlayer(com) }

    val players = EPlayer.entries.associateWith {
        playersFactory(it) { m: PlayerMessage -> receivePlayer(WrappedPlayerMessage(it, m)) }
    }
    val rp = GameRoundPlay(::receiveServer, deck, null, soFareMoves, "Sim${deck.hashCode().toHexString(hexhex)}")

    fun start(
    ): SimpleBattle.Linfo {

        rp.start()

        var starved = 0

        while (rp.state == GameRoundPlay.State.RUNNING) {
            try {
                val sm = serverQueue.removeFirstOrNull()
                if (sm != null) {
                    history.add(sm)
                    players.getValue(sm.u).receiveMessage(sm.message, sm.u)
                }
                val pm = playersQueue.removeFirstOrNull()
                if (pm != null) {
                    history.add(pm)
                    starved = 0
                    rp.receivePlayerMessage(pm)
                } else {
                    starved++
                }
//                if (starved > 100) {
//                    rp.sendTableAndHandcards()
//                    starved++
//                }
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

