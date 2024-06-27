package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.common.Player

interface IPlayLogEntry {
    // ugly but still need to figure out subtypes cleanly
    // maybe afterall don't make multiple interfaces...
    // just have type and nullable cards and stuff
    val type: String
    val player: Player


    /**
     * Move Entries turn over right of play
     */
    interface MoveEntry : IPlayLogEntry


    data class RegularMoveEntry(override val player: Player, val cards: Collection<PlayCard>) :
        MoveEntry {
        constructor(player: Player, card: PlayCard) : this(player, listOf(card))

        init {
            if (cards.isEmpty()) {
                throw IllegalArgumentException("Use Pass")
            }
        }

        override val type = "RegularMove"
        override fun toString(): String = "${player.name}:$cards"
    }

    data class PassMoveEntry(override val player: Player) : MoveEntry {
        override val type = "Pass"
    }

    data class BombEntry(override val player: Player, val cards: List<PlayCard>) : MoveEntry {
        override val type = "Bomb"
        override fun toString(): String = "B:$player:$cards"
    }

    data class PlayerFinishedEntry(override val player: Player) : IPlayLogEntry {
        override val type = "Finished"
        override fun toString(): String = "F:$player"
    }

    data class SmallTichuEntry(override val player: Player) : IPlayLogEntry {
        override val type = "Tichu"
        override fun toString(): String = "t:$player"
    }

    data class BigTichuEntry(override val player: Player) : IPlayLogEntry {
        override val type = "BigTichu"
        override fun toString(): String = "T:$player"
    }

    data class WishedEntry(override val player: Player, val value: Int) : IPlayLogEntry {
        override val type = "Wished"
        override fun toString(): String = "w:$player:$value"
    }

    data class WishFullfilledEntry(override val player: Player, val value: Int) : IPlayLogEntry {
        override val type = "WishFullfilled"
        override fun toString(): String = "W:$player:$value"
    }

    data class DrgGiftedEntry(override val player: Player, val to: Player) : IPlayLogEntry {
        override val type = "DrgGift"
        override fun toString(): String = "D:$player->$to"
    }

}