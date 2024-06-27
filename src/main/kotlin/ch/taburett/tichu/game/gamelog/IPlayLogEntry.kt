package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.common.EPlayer

interface IPlayLogEntry {
    // ugly but still need to figure out subtypes cleanly
    // maybe afterall don't make multiple interfaces...
    // just have type and nullable cards and stuff
    val type: String
    val player: EPlayer


    /**
     * Move Entries turn over right of play
     */
    interface MoveEntry : IPlayLogEntry


    data class RegularMoveEntry(override val player: EPlayer, val cards: Collection<PlayCard>) :
        MoveEntry {
        constructor(player: EPlayer, card: PlayCard) : this(player, listOf(card))

        init {
            if (cards.isEmpty()) {
                throw IllegalArgumentException("Use Pass")
            }
        }

        override val type = "RegularMove"
        override fun toString(): String = "${player.name}:$cards"
    }

    data class PassMoveEntry(override val player: EPlayer) : MoveEntry {
        override val type = "Pass"
    }

    data class BombEntry(override val player: EPlayer, val cards: List<PlayCard>) : MoveEntry {
        override val type = "Bomb"
        override fun toString(): String = "B:$player:$cards"
    }

    data class PlayerFinishedEntry(override val player: EPlayer) : IPlayLogEntry {
        override val type = "Finished"
        override fun toString(): String = "F:$player"
    }

    data class SmallTichuEntry(override val player: EPlayer) : IPlayLogEntry {
        override val type = "Tichu"
        override fun toString(): String = "t:$player"
    }

    data class BigTichuEntry(override val player: EPlayer) : IPlayLogEntry {
        override val type = "BigTichu"
        override fun toString(): String = "T:$player"
    }

    data class WishedEntry(override val player: EPlayer, val value: Int) : IPlayLogEntry {
        override val type = "Wished"
        override fun toString(): String = "w:$player:$value"
    }

    data class WishFullfilledEntry(override val player: EPlayer, val value: Int) : IPlayLogEntry {
        override val type = "WishFullfilled"
        override fun toString(): String = "W:$player:$value"
    }

    data class DrgGiftedEntry(override val player: EPlayer, val to: EPlayer) : IPlayLogEntry {
        override val type = "DrgGift"
        override fun toString(): String = "D:$player->$to"
    }

}