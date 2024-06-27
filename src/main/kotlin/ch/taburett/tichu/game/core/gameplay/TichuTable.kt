package ch.taburett.tichu.game.core.gameplay

import ch.taburett.tichu.game.gamelog.IPlayLogEntry

open class TichuTable(moves: List<IPlayLogEntry>) : ITichuTable {
    override val moves = moves.toList()
    override fun toString(): String {
        return moves.joinToString("|")
    }
}