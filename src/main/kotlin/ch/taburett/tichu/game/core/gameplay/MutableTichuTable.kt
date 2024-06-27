package ch.taburett.tichu.game.core.gameplay

import ch.taburett.tichu.game.gamelog.IPlayLogEntry

/**
 * Current open trick
 */
class MutableTichuTable(moves: ITichuTable? = null) : ITichuTable {

    override val moves = moves?.moves?.toMutableList() ?: mutableListOf()

    fun add(played: IPlayLogEntry) {
        moves.add(played)
    }

    fun immutable(): ITichuTable {
        return TichuTable(moves)
    }

    override fun toString(): String {
        return moves.toString()
    }
}