package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.game.core.gameplay.TichuTable

interface Tricks {
    val tricks: List<Trick>
    val table: TichuTable
}