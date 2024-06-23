package ch.taburett.tichu.game

interface ImmutableTricks {
    val tricks: List<Trick>
    val table: ImmutableTable
}