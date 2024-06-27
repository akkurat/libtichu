package ch.taburett.tichu.game

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.gamelog.IPlayLogEntry
import ch.taburett.tichu.game.gamelog.IPlayLogEntry.*
import ch.taburett.tichu.game.gamelog.Trick

/**
 * Current open trick
 */
class MutableTichuTable(moves: TichuTable? = null) : TichuTable {

    override val moves = moves?.moves?.toMutableList() ?: mutableListOf()

    fun add(played: IPlayLogEntry) {
        moves.add(played)
    }

    fun immutable(): TichuTable {
        return TichuTableImpl(moves)
    }

    override fun toString(): String {
        return moves.toString()
    }
}

open class TichuTableImpl(moves: List<IPlayLogEntry>) : TichuTable {
    override val moves = moves.toList()
    override fun toString(): String {
        return moves.joinToString("|")
    }
}

interface TichuTable {
    val moves: List<IPlayLogEntry>

    fun toTrick(): Trick = Trick(moves.toList())

    fun isNotEmpty(): Boolean = moves.isNotEmpty()
    fun isEmpty(): Boolean = moves.isEmpty()
    fun allPass(activePlayers: Set<Player>): Boolean {

        val passedPlayers = mutableSetOf<Player>()

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
