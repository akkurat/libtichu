package ch.taburett.tichu.game

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.IPlayLogEntry.*

/**
 * Current open trick
 */
class MutableTable(moves: ImmutableTable? = null) : ImmutableTable {

    override val moves = moves?.moves?.toMutableList() ?: mutableListOf()

    fun add(played: IPlayLogEntry) {
        moves.add(played)
    }

    fun immutable(): ImmutableTable {
        return ImmutableTableImpl(moves)
    }

    override fun toString(): String {
        return moves.toString()
    }
}

open class ImmutableTableImpl(moves: List<IPlayLogEntry>) : ImmutableTable {
    override val moves = moves.toList()
    override fun toString(): String {
        return moves.joinToString("|")
    }
}

interface ImmutableTable {
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