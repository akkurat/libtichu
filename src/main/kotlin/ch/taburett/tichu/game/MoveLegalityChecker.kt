package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.cards.pattern
import ch.taburett.tichu.patterns.LegalType
import ch.taburett.tichu.patterns.LegalType.ILLEGAL
import ch.taburett.tichu.patterns.LegalType.WISH
import ch.taburett.tichu.patterns.LegalityAnswer
import ch.taburett.tichu.patterns.ok

/**
 * Assuming cards are incoming from an untrusted user input deserialization
 * hence we need to check if the player / user even had this card
 */
fun playedCardsValid(
    _tableCards: List<PlayCard>,
    cardsTriedToPlay: Collection<PlayCard>,
    handCards: List<HandCard>,
    wish: Int? = null,
): LegalityAnswer {

    if (_tableCards.any(cardsTriedToPlay::contains)) {
        return LegalityAnswer(LegalType.CHEATING, "Table and hand cannot be the same cards")
    }

    if (!handCards.containsAll(cardsTriedToPlay.map { c -> c.asHandcard() })) {
        return LegalityAnswer(LegalType.CHEATING, "You can only play cards of your hand")
    }

    if (_tableCards.isEmpty() && cardsTriedToPlay.isEmpty()) {
        return LegalityAnswer(ILLEGAL, "Cannot open with pass")
    }

    val pTAble = pattern(_tableCards)
    try {
        val pPlayed = pattern(cardsTriedToPlay)
        if (wish != null && handCards.filterIsInstance<NumberCard>().any { c -> c.getValue() == wish }) {
            if (cardsTriedToPlay.none { c -> c.getValue() == wish }) {
                if (_tableCards.isEmpty()) { // allready checked that player has wished card
                    return LegalityAnswer(WISH, "Wish $wish is pending. You're not allowed to play $cardsTriedToPlay")
                }
                val allPatternsMatchingTable = pTAble.type.factory.allPatterns(handCards)
                val possiblePatterns = allPatternsMatchingTable
                    .filter { p -> p.cards.any { c -> c.getValue() == wish } }
                if (possiblePatterns.isNotEmpty()) {
                    return LegalityAnswer(LegalType.WISH, possiblePatterns.first().toString())
                }
            }
        }

        if (cardsTriedToPlay.isEmpty()) {
            return ok()
        }
        return pPlayed.beats(pTAble);
    } catch (e: IllegalArgumentException) {
        return LegalityAnswer(ILLEGAL, "$cardsTriedToPlay is not valid pattern")
    }
}
