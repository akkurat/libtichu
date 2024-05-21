package tichu

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.playedCardsValid
import ch.taburett.tichu.patterns.LegalType
import ch.taburett.tichu.patterns.LegalType.*
import ch.taburett.tichu.patterns.LegalityAnswer
import ch.taburett.tichu.patterns.ok
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test if the new handhards are legal,
 * depending on the cards already played
 * in this trick and taking into account possible whishes
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoveLegalityCheckerTest {

    fun testBomb(): List<Arguments> {
        return listOf(
            arguments(OK, listOf(DRG), listOf(S2, J2, D2, P2), listOf(S4, S4, S2, J2, D2, P2)),
            arguments(OK, listOf(J3, S4, J5, P6, J7), listOf(S2, J2, D2, P2), listOf(S4, S4, S2, J2, D2, P2)),
            arguments(ILLEGAL, listOf(S3, J3, D3, P3), listOf(S2, J2, D2, P2), listOf(S4, S4, S2, J2, D2, P2))
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testBomb(
        exp: LegalType,
        table: List<PlayCard>,
        played: List<PlayCard>,
        hand: List<HandCard>,
    ) {
        assertEquals(exp, playedCardsValid(table, played, hand).type)
    }

    fun wrongLength(): List<Arguments> {
        return listOf(
            arguments(listOf(S3, J3, D3), listOf(S2, J2), listOf(S4, J4, S2, J2, D2, P2)),
            arguments(listOf(J3, D3), listOf(S4, J4, D4), listOf(D4, J4, S4, S2, J2, D2, P2))
        )
    }

    @ParameterizedTest
    @MethodSource
    fun wrongLength(table: List<PlayCard>, played: List<PlayCard>, hand: List<HandCard>) {
        assertEquals(ILLEGAL, playedCardsValid(table, played, hand).type)
    }

    @Test
    fun testItself() {
        assertAll({
            assertEquals(
                LegalityAnswer(OK, ""),
                playedCardsValid(listOf(), listOf(S2, J2), listOf(S4, S4, S2, J2, D2, P2))
            )
        })

    }

    fun testWishNotFullfilled(): Iterable<Arguments> {
        return listOf(
            arguments(
                LegalityAnswer(WISH, "STRAIGHT[P2, S3, P4, P5, J6, J7]"),
                listOf(MAH, S2, P3, S4, J5),
                listOf(P2, S3, P4, P5, J6),
                listOf(P2, S3, P4, P5, J6, J7),
                7
            ),
            arguments(
                LegalityAnswer(WISH, "SINGLE[P13]"),
                listOf(MAH),
                listOf(S2),
                listOf(P4, S4, S2, J2, D2, P2, P13),
                13
            ),
            arguments(
                LegalityAnswer(WISH, "PAIR[P4, S4]"),
                listOf(S2, P2),
                listOf(S3, J3),
                listOf(P4, S4, S3, J3, D2, P2, P13),
                4
            ),
            arguments(
                LegalityAnswer(WISH, "TRIPLE[P4, S4, J4]"),
                listOf(S2, P2, J2),
                listOf(S3, J3, P3),
                listOf(P4, S4, J4, S3, J3, P3, D2, P2, P13),
                4
            ),
            arguments(
                LegalityAnswer(WISH, "PAIR[P4, PHX4]"),
                listOf(S2, P2),
                listOf(S3, J3),
                listOf(P4, S3, J3, D2, P2, P13, PHX),
                4
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testWishNotFullfilled(
        exp: LegalityAnswer,
        table: List<PlayCard>,
        played: List<PlayCard>,
        hand: List<HandCard>,
        wish: Int,
    ) {
        assertEquals(exp, playedCardsValid(table, played, hand, wish))
    }


    fun testWishOk(): Iterable<Arguments> {
        val handcards = listOf(S2, S3, D4, S5, D6, PHX, J8, J14, S14)
        return listOf(
            arguments(listOf(MAH), listOf(S5), handcards, 5),
            arguments(listOf(MAH, P2, P3, S4, D5), listOf(S2, S3, D4, S5, D6), handcards, 7),
            arguments(listOf(P2, D2), listOf(J14, S14), handcards, 7),
            arguments(listOf(P2, D2), listOf(PHX.asPlayCard(8), J8), handcards, 8),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun testWishOk(table: List<PlayCard>, played: List<PlayCard>, hand: List<HandCard>, wish: Int) {
        assertEquals(ok(), playedCardsValid(table, played, hand, wish))
    }

    @Test
    fun dog() {
        assertAll(
            "Doggy dog",
            {
                assertEquals(
                    LegalityAnswer(ILLEGAL, "SINGLE[DOG] is not higher than SINGLE[MAH]"),
                    playedCardsValid(listOf(MAH), listOf(DOG), listOf(DOG), 6),
                    "Dog"
                )
            },
            {
                assertEquals(
                    LegalityAnswer(WISH, "Wish 6 is pending. You're not allowed to play [DOG]"),
                    playedCardsValid(listOf(), listOf(DOG), listOf(DOG, J6), 6),
                    "Wish"
                )
            },
            {
                assertEquals(
                    ok(),
                    playedCardsValid(listOf(), listOf(DOG), listOf(DOG), 6),
                    "quark"
                )
            })
    }

    @Test
    fun endRound() {

    }

    @Test
    fun cannotPassAusspiel() {
        assertEquals(
            LegalityAnswer(ILLEGAL, "Cannot open with pass"),
            playedCardsValid(listOf(), listOf(), listOf(MAH, S5))
        )
    }

}