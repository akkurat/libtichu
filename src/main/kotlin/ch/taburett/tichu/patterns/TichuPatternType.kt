package ch.taburett.tichu.patterns

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard

enum class TichuPatternType(val factory: PatternImplFactory) {
    // Anspiel = Pass? not really.. puh
    ANSPIEL(Empty),
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

    // hm.. is param for phx really necessary? just filter it out?
    // same would work for height... just filter the lower cards..?
    fun allPatterns(cards: Collection<HandCard>, cardinality: Int? = null, incPhx: Boolean = true, ): Set<TichuPattern> {
        return factory.allPatterns(cards, cardinality, incPhx)
    }


}