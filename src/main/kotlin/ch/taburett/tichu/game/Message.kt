package ch.taburett.tichu.game

import ch.taburett.tichu.cards.DRG
import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.GameStage.Stage.*
import ch.taburett.tichu.patterns.Bomb
import ch.taburett.tichu.patterns.BombStraight

sealed class Message

sealed class ServerMessage : Message()

sealed class PlayerMessage : Message()

/**
 * Serverside Communication
 * all cards with the new stage are given, e.g.
 * EIGHT_CARDS: [1 2 3...8 ]
 * ALL_CARDS: [1 2 3 ... 14]
 * SCHUPFED: [1 2 3 ... 14]
 * GIFT_DRAGON [5,As,DRG]
 */
data class GameStage(val stage: Stage, val cards: List<HandCard>) : ServerMessage() {
    enum class Stage { EIGHT_CARDS, ALL_CARDS, SCHUPFED, GIFT_DRAGON }
    init {
        when(stage) {
            EIGHT_CARDS -> if (cards.size != 8) throw IllegalArgumentException("Not eight cards")
            ALL_CARDS, SCHUPFED -> if(cards.size != 14) throw IllegalArgumentException("Not 14 cards")
            GIFT_DRAGON -> if( !cards.contains(DRG) ) throw IllegalArgumentException("Dragon is not part of the cards")
        }
    }
}

data class Wish(val value: Int): PlayerMessage()
{
    init {
        if (value <2 || value >14) {
            throw IllegalArgumentException("Value must be between 2 and 14, inclusive")
        }
    }
}

// id or not?
// try first without ids and go with implicit
sealed class Ack(val answer: Boolean ): PlayerMessage() {
    class BigTichu(answer: Boolean) : Ack(answer)
    class TichuBeforeSchupf(answer: Boolean) : Ack(answer)
    class TichuBeforePlay(answer: Boolean) : Ack(answer)
}

data class GiftDragon(val who: Player): PlayerMessage()

data class Schupf(val re: HandCard, val li: HandCard, val partner: HandCard) : PlayerMessage()

data class Bomb(val cards: List<PlayCard>) : PlayerMessage() {
    init {
        val b = Bomb.pattern(cards)
        val sb = BombStraight.pattern(cards)
        if( b == null && sb == null) {
            throw IllegalArgumentException("no bomb")
        }
    }
}

object Tichu: PlayerMessage()
object BigTichu: PlayerMessage()

data class Move(val cards: List<HandCard>) : PlayerMessage()




/**
 *
 */
