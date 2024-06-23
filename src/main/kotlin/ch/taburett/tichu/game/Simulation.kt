@file:OptIn(ExperimentalStdlibApi::class)

package ch.taburett.tichu.game

import ch.taburett.tichu.game.player.BattleRound.AutoPlayer
import ch.taburett.tichu.game.player.SimpleBattle
import ch.taburett.tichu.game.player.StupidPlayer
import ch.taburett.tichu.game.player.hexhex
import ch.taburett.tichu.game.protocol.Message
import ch.taburett.tichu.game.protocol.Message.PlayerMessage

class SimulationRound(

    _deck: Deck? = null,
    soFareMoves: MutableTricks,
    _playersFactory: ((a: Player, b: (PlayerMessage) -> Unit) -> AutoPlayer)? = null,
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

    val groupFactory = PlayerGroup.entries.associateWith { p -> }

    val playersFactory = _playersFactory ?: { a, com -> StupidPlayer(com) }

    val players = Player.entries.associateWith {
        playersFactory(it) { m: PlayerMessage -> receivePlayer(WrappedPlayerMessage(it, m)) }
    }
    val rp = RoundPlay(::receiveServer, deck, null, soFareMoves, "Sim${deck.hashCode().toHexString(hexhex)}")

    fun start(
    ): SimpleBattle.Linfo {

        rp.start()

        var starved = 0

        while (rp.state == RoundPlay.State.RUNNING) {
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

