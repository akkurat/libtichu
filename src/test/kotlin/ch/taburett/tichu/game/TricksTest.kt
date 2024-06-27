package ch.taburett.tichu.game

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.gamelog.IPlayLogEntry.*
import ch.taburett.tichu.game.Player.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TricksTest {
    @Test
    fun testRegularNext() {
        val tricks = MutableTricks(null)
        tricks.add(RegularMoveEntry(A1, MAH))
        val cmap = createDeck()
        val mdeck = mutableDeck(cmap)
        val nplayer = tricks.nextPlayer(mdeck)

        assertEquals(B1, nplayer)
    }

    @Test
    fun testDrgNext() {
        val tricks = MutableTricks(null)

        tricks.add(RegularMoveEntry(A1, DRG))
        tricks.add(PassMoveEntry(B1))
        tricks.add(PassMoveEntry(A2))
        tricks.add(PassMoveEntry(B2))
        tricks.add(DrgGiftedEntry(A1, B2))

        val cmap = createDeck()

        val mdeck = mutableDeck(cmap)
        mdeck.playCards(A1, listOf(DRG))
        val nplayer = tricks.nextPlayer(mdeck)

        assertEquals(A1, nplayer)
    }

    @Test
    fun testDogNext() {
        val tricks = MutableTricks(null)

        tricks.add(RegularMoveEntry(A1, DOG))

        // A1 has all special cards like this
        val cmap = createDeck()

        val mdeck = mutableDeck(cmap)
        mdeck.playCards(A1, listOf(DOG))
        tricks.endTrick()
        val nplayer = tricks.nextPlayer(mdeck)

        assertEquals(A2, nplayer)
    }

    @Test
    fun testTichuNext() {
        val tricks = MutableTricks(null)

        val cmap = createDeck()
        tricks.add(SmallTichuEntry(A1))

        // A1 has all special cards like this

        val mdeck = mutableDeck(cmap)
        val nplayer = tricks.nextPlayer(mdeck)

        assertEquals(A1, nplayer)
    }

    @Test
    fun testBombNext() {
        val tricks = MutableTricks(null)

        val cmap = createDeck()
        tricks.add(RegularMoveEntry(A1, DRG))
        tricks.add(PassMoveEntry(B1))
        tricks.add(BombEntry(A1, listOf(S2, S3, S4, S5, S6)))

        // A1 has all special cards like this

        val mdeck = mutableDeck(cmap)
        val nplayer = tricks.nextPlayer(mdeck)

        assertEquals(B1, nplayer)
    }

    private fun mutableDeck(cmap: Map<Player, List<HandCard>>): MutableDeck {
        val mdeck = MutableDeck.createInitial(cmap)
        return mdeck
    }

    private fun createDeck(): Map<Player, List<HandCard>> {
        val cmap = playerList.zip(fulldeck.chunked(14)).toMap()
        return cmap
    }
}