package ch.taburett.tichu.game

import ch.taburett.tichu.cards.PlayCard

/**
 * Current open trick
 */
class Table( var currentPlayer: Player) {

    fun toBeat(): Played {
        return  moves.filterIsInstance<Played>().last { !it.pass }
    }

    fun toBeatCards(): Collection<PlayCard> {
        return if( isNotEmpty() ) toBeat().cards else emptyList()
    }

    val moves = ArrayList<IPlayed>()

    fun toTrick(): Trick = Trick(moves.toList())

    fun isNotEmpty(): Boolean = moves.isNotEmpty()
    fun isEmpty(): Boolean = moves.isEmpty()
    fun add(played: IPlayed) {
        moves.add(played)
    }

    fun allPass(activePlayers: Set<Player>): Boolean {

        val passedPlayers = mutableSetOf<Player>()

        for (p in moves.reversed().filterIsInstance<Played>()) {
            if (p.pass) {
                passedPlayers.add(p.player)
            } else {
                val playerMove = p.player
                return passedPlayers.containsAll(activePlayers.minus(playerMove))
            }
        }
        return false

    }
    fun nextLegalMove() {

    }

}