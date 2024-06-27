package ch.taburett.tichu.game

import ch.taburett.tichu.cards.MAH
import ch.taburett.tichu.cards.S2
import ch.taburett.tichu.game.core.gameplay.MutableTichuTable
import ch.taburett.tichu.game.core.common.Player
import ch.taburett.tichu.game.gamelog.IPlayLogEntry
import ch.taburett.tichu.game.core.common.Player.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableTichuTableTest {
    @Test
    fun allPassTrue() {
        val table = MutableTichuTable()
        table.add(IPlayLogEntry.RegularMoveEntry(A1, MAH))
        table.add(IPlayLogEntry.PassMoveEntry(B1))
        table.add(IPlayLogEntry.PassMoveEntry(A2))
        table.add(IPlayLogEntry.PassMoveEntry(B2))

        assertTrue { table.allPass(Player.entries.toSet()) }
    }

    @Test
    fun allPassFalse() {
        val table = MutableTichuTable()
        table.add(IPlayLogEntry.RegularMoveEntry(A1, MAH))
        table.add(IPlayLogEntry.PassMoveEntry(B1))
        table.add(IPlayLogEntry.PassMoveEntry(A2))
        table.add(IPlayLogEntry.RegularMoveEntry(B2, S2))
        table.add(IPlayLogEntry.PassMoveEntry(A1))
        table.add(IPlayLogEntry.PassMoveEntry(B1))

        assertFalse { table.allPass(Player.entries.toSet()) }

    }
}