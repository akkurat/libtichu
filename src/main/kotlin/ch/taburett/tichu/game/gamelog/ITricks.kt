package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.game.core.gameplay.ITichuTable

interface ITricks {
    val tricks: List<Trick>
    val table: ITichuTable
}