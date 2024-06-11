package ch.taburett.tichu.game

import ch.taburett.tichu.cards.PlayCard

/**
 * Current open trick
 */
class Table(moves: ImmutableTable? = null) : ImmutableTable {

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

        for (p in moves.reversed().filterIsInstance<RegularMoveEntry>()) {
            if (p.pass) {
                passedPlayers.add(p.player)
            } else {
                val playerMove = p.player
                return passedPlayers.containsAll(activePlayers.minus(playerMove))
            }
        }
        return false

    }

    fun toBeat(): RegularMoveEntry {
        return moves.filterIsInstance<RegularMoveEntry>().last { !it.pass }
    }

    fun toBeatCards(): Collection<PlayCard> {
        return if (isNotEmpty()) toBeat().cards else emptyList()
    }

    fun allCards(): List<PlayCard> {
        return moves.filterIsInstance<RegularMoveEntry>().flatMap { it.cards }

    }
}
