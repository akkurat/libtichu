package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.game.ImmutableTable
import ch.taburett.tichu.game.ImmutableTricks

class Tricks(override val tricks: List<Trick>, override val table: ImmutableTable): ImmutableTricks