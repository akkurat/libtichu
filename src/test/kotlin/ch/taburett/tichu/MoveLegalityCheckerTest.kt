package ch.taburett.tichu

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.LegalType
import ch.taburett.tichu.game.LegalityAnswer
import ch.taburett.tichu.game.playedCardsValid
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Test if the new handhards are legal,
 * depending on the cards already played
 * in this trick and taking into account possible whishes
 */
class MoveLegalityCheckerTest {

    @Test
    fun testBomb() {
        assertAll({
            assertEquals( LegalType.OK,  playedCardsValid(listOf(DRG), listOf(S2, J2, D2, P2), listOf(S4, S4, S2, J2, D2, P2)).type )
            assertEquals( LegalType.OK,  playedCardsValid(listOf(J3,S4,J5,P6,J7), listOf(S2, J2, D2, P2), listOf(S4, S4, S2, J2, D2, P2)).type )
            assertEquals( LegalType.ILLEGAL,  playedCardsValid(listOf(S3,J3,D3,P3), listOf(S2, J2, D2, P2), listOf(S4, S4, S2, J2, D2, P2)).type )
        })
    }

    @Test
    fun testLength() {

        assertAll({
            assertEquals( LegalType.ILLEGAL,  playedCardsValid(listOf(S3,J3,D3), listOf(S2, J2 ), listOf(S4, S4, S2, J2, D2, P2)).type )
        })
    }

    @Test
    fun testItself() {
        assertAll({
            assertEquals(LegalityAnswer(LegalType.OK, ""), playedCardsValid(listOf(), listOf(S2, J2), listOf(S4, S4, S2, J2, D2, P2)))
        })

    }

    @Test
    fun testWishNotFullfilled() {
        assertAll({
            // single
            assertEquals(LegalType.WISH, playedCardsValid(listOf(MAJ), listOf(S2), listOf(P4, S4, S2, J2, D2, P2,P13),13).type)
            // pair
            assertEquals(LegalType.WISH, playedCardsValid(listOf(S2,P2), listOf(S3,J3), listOf(P4, S4, S3, J3, D2, P2,P13),4).type)
            // triple
            assertEquals(LegalityAnswer(LegalType.WISH, "[P4,S4,J4]"), playedCardsValid(listOf(S2,P2,J2), listOf(S3,J3,P3), listOf(P4, S4, J4, S3, J3,P3, D2, P2,P13),4))
            // straight
            assertEquals(LegalityAnswer(LegalType.WISH,"[P2, S3, P4, P5, J6, J7]"), playedCardsValid( listOf(MAJ, S2, P3, S4, J5), listOf(P2, S3, P4, P5, J6), listOf(P2, S3, P4, P5, J6, J7), 7 ))

        })
    }

}