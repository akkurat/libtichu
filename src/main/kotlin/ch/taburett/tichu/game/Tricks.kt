package ch.taburett.tichu.game

import ch.taburett.tichu.cards.DOG

class Tricks(lastMoves: ImmutableTable?) {

    private val _tricks = mutableListOf<Trick>()
    val tricks: List<Trick>
        get() = _tricks
    private var _table = Table(lastMoves)
    val table: ImmutableTable
        get() = _table.immutable()

    fun endTrick() {
        if(table.toTrick().moves.isNotEmpty()) {
            _tricks.add(table.toTrick())
            _table = Table()
        }
    }

    fun nextPlayer(deck: Deck): Player {
        if (tricks.isEmpty()) {
            return if (table.moves.filter { it !is Wished }.isEmpty()) {
                deck.initialPlayer
            } else {
                val lastPlayer = table.moves.last().player
                deck.nextPlayer(lastPlayer)
            }
        } else {
            if (table.moves.filter { it !is Wished }.isEmpty()) {
                val lastMove = tricks.last().moves.last()
                if (lastMove is RegularMoveEntry) {
                    if (lastMove.cards.contains(DOG)) {
                        return deck.nextPlayer(lastMove.player, 2)
                    }
                }
                return deck.nextPlayer(lastMove.player)
            } else {
                return deck.nextPlayer(table.moves.last(::notWish).player)
            }
        }
    }
    val orderWinning get() = tricks.flatMap { it.playerFinished }

    private fun notWish(it: IPlayLogEntry): Boolean = it !is Wished
    fun add(logEntry: IPlayLogEntry) {
        _table.add(logEntry)
    }

}
