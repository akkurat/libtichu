package ch.taburett.tichu

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.game.MutableRound
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class Ha {
    @Test
    fun test8Cards() {
        val round = MutableRound()
        val cardsCheck = round.cardMap.values
            .map { c -> Executable { assertEquals(14, c.size) } }
            .toList()

        assertAll( //                () -> assertEquals(BIG_TICHU, round.getState()),
            { assertAll(cardsCheck) })
    }
}
