@file:JsExport
package ch.taburett.tichu.cards

import kotlin.js.JsExport


val PHX = Phoenix()
val DRG = SpecialCard("Dragon", "DRG", 17, 25);
val MAJ = SpecialCard("MahJong", "MAJ", 1);
val DOG = SpecialCard("Dog","DOG", 0);

val J2 = NumberCard(2, Color.JADE)
val J3 = NumberCard(3, Color.JADE)
val J4 = NumberCard(4, Color.JADE)
val J5 = NumberCard(5, Color.JADE, 5)
val J6 = NumberCard(6, Color.JADE)
val J7 = NumberCard(7, Color.JADE)
val J8 = NumberCard(8, Color.JADE)
val J9 = NumberCard(9, Color.JADE)
val J10 = NumberCard(10, Color.JADE, 10)
val J11 = NumberCard(11, Color.JADE, 0, "J")
val J12 = NumberCard(12, Color.JADE, 0, "Q")
val J13 = NumberCard(13, Color.JADE, 10, "K")
val J14 = NumberCard(14, Color.JADE, 0, "A")

val D2 = NumberCard(2, Color.SWORDS)
val D3 = NumberCard(3, Color.SWORDS)
val D4 = NumberCard(4, Color.SWORDS)
val D5 = NumberCard(5, Color.SWORDS, 5)
val D6 = NumberCard(6, Color.SWORDS)
val D7 = NumberCard(7, Color.SWORDS)
val D8 = NumberCard(8, Color.SWORDS)
val D9 = NumberCard(9, Color.SWORDS)
val D10 = NumberCard(10, Color.SWORDS, 10)
val D11 = NumberCard(11, Color.SWORDS, 0, "J")
val D12 = NumberCard(12, Color.SWORDS, 0, "Q")
val D13 = NumberCard(13, Color.SWORDS, 10, "K")
val D14 = NumberCard(14, Color.SWORDS, 0, "A")

val P2 = NumberCard(2, Color.PAGODAS)
val P3 = NumberCard(3, Color.PAGODAS)
val P4 = NumberCard(4, Color.PAGODAS)
val P5 = NumberCard(5, Color.PAGODAS, 5)
val P6 = NumberCard(6, Color.PAGODAS)
val P7 = NumberCard(7, Color.PAGODAS)
val P8 = NumberCard(8, Color.PAGODAS)
val P9 = NumberCard(9, Color.PAGODAS)
val P10 = NumberCard(10, Color.PAGODAS, 10)
val P11 = NumberCard(11, Color.PAGODAS, 0, "J")
val P12 = NumberCard(12, Color.PAGODAS, 0, "Q")
val P13 = NumberCard(13, Color.PAGODAS, 10, "K")
val P14 = NumberCard(14, Color.PAGODAS, 0, "A")

val S2 = NumberCard(2, Color.STARS)
val S3 = NumberCard(3, Color.STARS)
val S4 = NumberCard(4, Color.STARS)
val S5 = NumberCard(5, Color.STARS, 5)
val S6 = NumberCard(6, Color.STARS)
val S7 = NumberCard(7, Color.STARS)
val S8 = NumberCard(8, Color.STARS)
val S9 = NumberCard(9, Color.STARS)
val S10 = NumberCard(10, Color.STARS, 10)
val S11 = NumberCard(11, Color.STARS, 0, "J")
val S12 = NumberCard(12, Color.STARS, 0, "Q")
val S13 = NumberCard(13, Color.STARS, 10, "K")
val S14 = NumberCard(14, Color.STARS, 0, "A")

val fulldeck = listOf(

    PHX, DRG, MAJ, DOG,
    S2, S3, S4, S5, S6, S7, S8, S9, S10, S11, S12, S13, S14,
    J2, J3, J4, J5, J6, J7, J8, J9, J10, J11, J12, J13, J14,
    D2, D3, D4, D5, D6, D7, D8, D9, D10, D11, D12, D13, D14,
    P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14
)

// todo: lookup by King / queen instead of only 12/13
val lookupByName = fulldeck.associateBy { k -> k.toString() }


