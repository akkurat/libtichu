package ch.taburett.tichu.game.core.gameplay

import ch.taburett.tichu.cards.MAH
import ch.taburett.tichu.cards.S2
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.gamelog.IPlayLogEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableITichuTableTest {
    @Test
    fun allPassTrue() {
        val table = MutableTichuTable()
        table.add(IPlayLogEntry.RegularMoveEntry(EPlayer.A1, MAH))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.B1))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.A2))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.B2))

        assertTrue { table.allPass(EPlayer.entries.toSet()) }
    }

    @Test
    fun allPassFalse() {
        val table = MutableTichuTable()
        table.add(IPlayLogEntry.RegularMoveEntry(EPlayer.A1, MAH))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.B1))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.A2))
        table.add(IPlayLogEntry.RegularMoveEntry(EPlayer.B2, S2))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.A1))
        table.add(IPlayLogEntry.PassMoveEntry(EPlayer.B1))

        assertFalse { table.allPass(EPlayer.entries.toSet()) }

    }
}