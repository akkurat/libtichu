package ch.taburett.tichu.game

import ch.taburett.tichu.cards.PlayCard

/**
 * Current open trick
 */
class Table : ImmutableTable {

    override val moves = ArrayList<IPlayLogEntry>()

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
        return moves.joinToString ("|")
    }
}

interface ImmutableTable {
    val moves: List<IPlayLogEntry>

    fun toTrick(): Trick = Trick(moves.toList())

    fun isNotEmpty(): Boolean = moves.isNotEmpty()
    fun isEmpty(): Boolean = moves.isEmpty()
    fun allPass(activePlayers: Set<Player>): Boolean {

        val passedPlayers = mutableSetOf<Player>()

        for (p in moves.reversed().filterIsInstance<PlayLogEntry>()) {
            if (p.pass) {
                passedPlayers.add(p.player)
            } else {
                val playerMove = p.player
                return passedPlayers.containsAll(activePlayers.minus(playerMove))
            }
        }
        return false

    }

    fun toBeat(): PlayLogEntry {
        return moves.filterIsInstance<PlayLogEntry>().last { !it.pass }
    }

    fun toBeatCards(): Collection<PlayCard> {
        return if (isNotEmpty()) toBeat().cards else emptyList()
    }
}
