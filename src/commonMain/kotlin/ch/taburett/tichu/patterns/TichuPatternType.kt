@file:JsExport()
package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import kotlin.js.JsExport

enum class TichuPatternType(val factory: PatternImplFactory) {
    EMPTY(Empty),
    SINGLE(Single),
    PAIR(Pair),
    TRIPLE(Triple),
    FULLHOUSE(FullHouse),
    STRAIGHT(Straight),
    STAIRS(Stairs),
    BOMB(Bomb),
    BOMBSTRAIGHT(BombStraight);

    fun pattern(cards: Collection<PlayCard>): TichuPattern? {
        return factory.pattern(cards);
    }

    fun patterns(cards: Collection<HandCard>): Set<TichuPattern> {
        return factory.allPatterns(cards)
    }


}