package ch.taburett.tichu.game.core.gameplay

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.gamelog.IPlayLogEntry
import ch.taburett.tichu.game.gamelog.IPlayLogEntry.*
import ch.taburett.tichu.game.gamelog.Trick

interface ITichuTable {
    val moves: List<IPlayLogEntry>

    fun toTrick(): Trick = Trick(moves.toList())

    fun isNotEmpty(): Boolean = moves.isNotEmpty()
    fun isEmpty(): Boolean = moves.isEmpty()
    fun allPass(activePlayers: Set<EPlayer>): Boolean {

        val passedPlayers = mutableSetOf<EPlayer>()

        for (p in moves.reversed().filter{it is RegularMoveEntry || it is PassMoveEntry} ) {
            if (p is PassMoveEntry) {
                passedPlayers.add(p.player)
            } else { // at first non-pass move
                val playerMove = p.player
                return passedPlayers.containsAll(activePlayers.minus(playerMove))
            }
        }
        return false
    }

    fun toBeat(): RegularMoveEntry? {
        return moves.filterIsInstance<RegularMoveEntry>().lastOrNull()
    }

    fun toBeatCards(): Collection<PlayCard> {
        return if (isNotEmpty()) toBeat()?.cards ?: emptyList() else emptyList()
    }

    fun allCards(): List<PlayCard> {
        return moves.filterIsInstance<RegularMoveEntry>().flatMap { it.cards }

    }
}
