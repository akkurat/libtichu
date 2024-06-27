package ch.taburett.tichu.game

import ch.taburett.tichu.game.gamelog.Trick

interface Tricks {
    val tricks: List<Trick>
    val table: TichuTable
}