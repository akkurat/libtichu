package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.cards.DOG
import ch.taburett.tichu.game.core.common.IDeck
import ch.taburett.tichu.game.core.gameplay.MutableTichuTable
import ch.taburett.tichu.game.core.gameplay.ITichuTable
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.gamelog.IPlayLogEntry.*

class MutableTricks(tricks: ITricks?) : ITricks {

    private val _tricks = tricks?.tricks?.toMutableList() ?: mutableListOf()
    override val tricks: List<Trick>
        get() = _tricks
    private var _Mutable_table = MutableTichuTable(tricks?.table)
    override val table: ITichuTable
        get() = _Mutable_table.immutable()

    fun endTrick() {
        if (table.toTrick().moves.isNotEmpty()) {
            _tricks.add(table.toTrick())
            _Mutable_table = MutableTichuTable()
        }
    }

    fun nextPlayer(deck: IDeck): EPlayer {
        // first trick
        if (tricks.isEmpty()) {
            // first regular move
            return if (table.moves.filterIsInstance<RegularMoveEntry>().none()) {
                deck.initialPlayer
            } else {
                val lastPlayer = table.moves.last { it is MoveEntry }.player
                deck.nextPlayer(lastPlayer)
            }
        } else {
            if (table.moves.none { it is MoveEntry }) {
                val lastMove = tricks.last().moves.last { it is MoveEntry }
                if (lastMove is RegularMoveEntry) {
                    if (lastMove.cards.contains(DOG)) {
                        return deck.nextPlayer(lastMove.player, 2)
                    }
                }
                return deck.nextPlayer(lastMove.player)
            } else {
                return deck.nextPlayer(table.moves.last { it is MoveEntry }.player)
            }
        }
    }

    val orderWinning get() = tricks.flatMap { it.playerFinishedEntry }

    fun add(logEntry: IPlayLogEntry) {
        _Mutable_table.add(logEntry)
    }

    fun immutable(): Tricks {
        return Tricks(tricks, table)
    }

}

