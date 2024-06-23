package tichu

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.*
import ch.taburett.tichu.game.IPlayLogEntry.*
import ch.taburett.tichu.game.Player.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import kotlin.test.Test

data class Entry<A, B>(override val key: A, override val value: B) : Map.Entry<A, B>

class TestBattleRoundInfo {
    @Test
    fun testNormalWin() {

        val a1 = setOf(MAH, S2, P3, S4, P5, D6, J7, J8, D9, S10, S11, J12, D13, P14)

        val b1_str = setOf(P2, D3, D4, D5, J6, S7)
        val b1_fh = setOf(S9) + P9 + S14 + D14 + J14
        val b1 = b1_str + DRG + PHX + J13 + b1_fh

        @Suppress("UNCHECKED_CAST")
        val a2 = (fulldeck - DOG - a1 - b1).shuffled().take(14) as List<PlayCard>

        val b2: List<HandCard> = (fulldeck - a1 - b1 - a2)

        val tricks = listOf(
            listOf(RegularMoveEntry(A1, a1.toList()), PlayerFinishedEntry(A1), p(B1), p(A2), p(B2)),
            listOf(RegularMoveEntry(B1, b1_str.toList()), p(A2), p(B2)),
            listOf(RegularMoveEntry(B1, b1_fh.toList()), p(A2), p(B2)),
            listOf(RegularMoveEntry(B1, DRG), p(A2), p(B2), DrgGiftedEntry(B1, A2)),
            listOf(RegularMoveEntry(B1, listOf(PHX.asPlayCard(13), J13)), PlayerFinishedEntry(B1), p(A2), p(B2)),
        ) + a2.dropLast(1).map { listOf(RegularMoveEntry(A2, it), p(B2)) } + a2.takeLast(1)
            .map { listOf(RegularMoveEntry(A2, it), PlayerFinishedEntry(A2)) }

        val initMap = mapOf(
            A1 to a1,
            A2 to a2,
            B1 to b1,
            B2 to b2
        )

        val laterMap = mapOf(
            A1 to listOf(),
            B1 to listOf(),
            A2 to listOf(),
            B2 to b2
        )

        val ri =
            RoundInfo(
                null,
                mapTricksToObj(tricks),
                initMap,
                laterMap,
                Player.entries.associateWith { ETichu.NONE },
                "test"
            )
        val points = ri.cardPoints

        val allCardsPlayed = tricks.flatten().filterIsInstance<RegularMoveEntry>().flatMap { it.cards }

        val allCardsLeft = laterMap.values.flatten()
        allCardsLeft.sumOf { it.getPoints() }


        val sumA = a1.sumOf { it.getPoints() } + a2.sumOf { it.getPoints() } + b2.sumOf { it.getPoints() } + 25
        val sumB = b1.sumOf { it.getPoints() } - 25
        println(points)
        assertAll(
            { assertThat(allCardsLeft.size + allCardsPlayed.size).isEqualByComparingTo(fulldeck.size) },
            { assertThat(points.values.sum()).isEqualTo(100) },
            { assertThat(points[PlayerGroup.A]).isEqualTo(sumA) },
            { assertThat(points[PlayerGroup.B]).isEqualTo(sumB) },
            { assertThat(ri.cards.getValue(PlayerGroup.A).contains(DRG)) },
            { assertThat(ri.cards.getValue(PlayerGroup.B).contains(PHX)) },
        )
    }

    private fun mapTricksToObj(tricks: List<List<IPlayLogEntry>>): MutableTricks {
        val tricksO = MutableTricks(null)
        for (trick in tricks) {
            trick.forEach { tricksO.add(it) }
            tricksO.endTrick()
        }
        return tricksO
    }

    @Test
    fun testDoubleWin() {

        val a1 = listOf(DOG, MAH, S2, P3, S4, P5, D6, J7, J8, D9, S10, S11, J12, D13)

        val a2_str = listOf(P2, D3, D4, D5, J6, S7)
        val a2_fh = setOf(S9) + D9 + S14 + D14 + J14
        val a2 = a2_str + DRG + PHX + J13 + a2_fh
        val (b1, b2) = (fulldeck - a1 - a2).shuffled().chunked(14)

        val tricks = listOf(
            listOf(RegularMoveEntry(A1, a1 - DOG), p(B1), p(A2), p(B2)),
            listOf(RegularMoveEntry(A1, DOG), PlayerFinishedEntry(A1), p(B1), p(A2), p(B2)),
            listOf(RegularMoveEntry(A2, a2_str), p(B2), p(B1)),
            listOf(RegularMoveEntry(A2, a2_fh.toList()), p(B2), p(B1)),
            listOf(RegularMoveEntry(A2, DRG), p(B2), p(B1)),
            listOf(RegularMoveEntry(A2, setOf(PHX.asPlayCard(13), J13)), PlayerFinishedEntry(A2))
        )

        val initMap = mapOf(
            A1 to a1,
            A2 to a2,
            B1 to b1,
            B2 to b2
        )
        val laterMap = mapOf(
            A1 to listOf(),
            A2 to listOf(),
            B1 to b1,
            B2 to b2
        )
        val ri =
            RoundInfo(
                null,
                mapTricksToObj(tricks),
                initMap,
                laterMap,
                Player.entries.associateWith { ETichu.NONE },
                "test"
            )
        val cardPoints = ri.cardPoints
        val bonus = ri.bonusPoints
        assertAll(
            { assertThat(cardPoints).containsExactly(Entry(PlayerGroup.A, 100), Entry(PlayerGroup.B, 0)) },
            { assertThat(bonus).containsExactly(Entry(PlayerGroup.A, 100), Entry(PlayerGroup.B, 0)) },

            )
    }

    private fun p(player: Player): PassMoveEntry = PassMoveEntry(player)
}


