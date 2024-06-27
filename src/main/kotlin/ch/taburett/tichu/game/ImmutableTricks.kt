package ch.taburett.tichu.game

import ch.taburett.tichu.game.gamelog.Trick

interface ImmutableTricks {
    val tricks: List<Trick>
    val table: ImmutableTable
}