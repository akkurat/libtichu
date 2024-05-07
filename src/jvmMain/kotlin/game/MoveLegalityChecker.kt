package game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.cards.pattern
import ch.taburett.tichu.patterns.LegalType
import ch.taburett.tichu.patterns.LegalityAnswer

/**
 * Assuming cards are incoming from an untrusted user input deserialization
 * hence we need to check if the player / user even had this card
 */
fun playedCardsValid(
    tableCards: List<PlayCard>,
    cardsTriedToPlay: List<PlayCard>,
    handCards: List<HandCard>,
    wish: Int? = null
): LegalityAnswer {

    if (tableCards.any(cardsTriedToPlay::contains)) {
        return LegalityAnswer(LegalType.CHEATING, "Table and hand cannot be the same cards")
    }

    if (!handCards.containsAll(cardsTriedToPlay.map { c -> c.asHandcard() })) {
        return LegalityAnswer(LegalType.CHEATING, "You can only play cards of your hand")
    }

    val pTAble = pattern(tableCards)
    val pPlayed = pattern(cardsTriedToPlay)
    if (wish != null && handCards.filterIsInstance<NumberCard>().any { c -> c.value() == wish }) {
        if (cardsTriedToPlay.none { c -> c.value() == wish }) {
            val allPatternsMatchingTable = pTAble.type.factory.allPatterns(handCards)
            val possiblePatterns = allPatternsMatchingTable
                .filter { p -> p.cards.any { c -> c.value() == wish } }
            if (possiblePatterns.isNotEmpty()) {
                return LegalityAnswer(LegalType.WISH, possiblePatterns.first().toString())
            }
        }
    }
    return pPlayed.beats(pTAble);
}
