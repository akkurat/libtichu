package tichu.player

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.player.weightPossibilitesNoRec
import ch.taburett.tichu.patterns.FullHouse
import ch.taburett.tichu.patterns.Single
import ch.taburett.tichu.patterns.Straight
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertAll
import kotlin.test.Test

class PlayerUtilsKtTest {

    @Test
    fun test() {

        val exp = weightPossibilitesNoRec(fulldeck.shuffled().take(14))
        println(exp)
    }

    @Test
    fun testFh() {
        val exp = weightPossibilitesNoRec(setOf(S2,D2,D6, J6, S6, S10, P10, D10, S8, P14,D14))
        val fh1 = FullHouse.pattern(setOf(D6, J6, S6, S10, P10))
        val fh2 = FullHouse.pattern(setOf(J6, S6, S10, P10, D10))
        val si = Single(P14)

        assertAll(
            { Assertions.assertThat(exp.getValue(fh1!!)).isLessThan(exp.getValue(fh2!!)) },
            { Assertions.assertThat(exp.getValue(fh1!!)).isLessThan(exp.getValue(si)) }
        )
        println(exp)
    }

    @Test
    fun testStraight() {
        val exp = weightPossibilitesNoRec(setOf(S2, D3, P4, J5, S6, S9, D10, J11, P12, J13, P14,D14,D2,S3))
        val s1 = Straight(setOf(S2, D3, P4, J5, S6))
        val s2 = Straight(setOf(S9, D10, J11, P12, J13,P14))

        println(exp)
        Assertions.assertThat(exp.getValue(s1)).isLessThan(exp.getValue(s2))

    }


    /*
        handcards = {ArrayList@9191}  size = 14
        0 = {NumberCard@11375} J6
        1 = {NumberCard@11376} S4
        2 = {NumberCard@11377} J11
        3 = {NumberCard@11378} P11
        4 = {NumberCard@11379} J3
        5 = {NumberCard@11216} D14
        6 = {NumberCard@11217} J5
        7 = {NumberCard@11380} D4
        8 = {NumberCard@11218} D8
        9 = {NumberCard@11219} D13
        10 = {NumberCard@11381} S6
        11 = {NumberCard@11382} P4
        12 = {NumberCard@11220} P12
        13 = {NumberCard@11383} D3

        {Pair@11283} PAIR[J6, S6] -> {Double@11359} 3.1068657468159078
        {Pair@11285} PAIR[S4, D4] -> {Double@11360} 1.7412903595848443
        {Pair@11287} PAIR[S4, P4] -> {Double@11361} 1.7412903595848443
        {Pair@11289} PAIR[D4, P4] -> {Double@11362} 1.7412903595848443
        {Pair@11291} PAIR[J11, P11] -> {Double@11363} 2.6845843480785048
        {Pair@11293} PAIR[J3, D3] -> {Double@11364} 3.122261185228885
        {Triple@11295} TRIPLE[S4, D4, P4] -> {Double@11365} 1.4712428178356085
        {FullHouse@11297} FULLHOUSE[S4, D4, P4, J6, S6] -> {Double@11366} 1.6382762896479743
        {FullHouse@11299} FULLHOUSE[S4, D4, P4, J11, P11] -> {Double@11367} 1.1137857677365253
        {FullHouse@11301} FULLHOUSE[S4, D4, P4, J3, D3] -> {Double@11368} 1.8609706027948438
        {Stairs@9194} STAIRS[J3, D3, S4, D4] -> {Double@11369} 1.4308721132367528

     */
}