package ch.taburett.tichu.game.protocol

import ch.taburett.tichu.cards.DRG
import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.IPlayed
import ch.taburett.tichu.game.Played
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.protocol.Stage.*
import ch.taburett.tichu.patterns.Bomb
import ch.taburett.tichu.patterns.BombStraight

interface Message

interface ServerMessage : Message {
}

interface PlayerMessage : Message

/**
 * Serverside Communication
 * all cards with the new stage are given, e.g.
 * EIGHT_CARDS: [1 2 3...8 ]
 * ALL_CARDS: [1 2 3 ... 14]
 * SCHUPFED: [1 2 3 ... 14]
 * GIFT_DRAGON [5,As,DRG]
 */
enum class Stage { EIGHT_CARDS, PRE_SCHUPF, POST_SCHUPF, GIFT_DRAGON, SCHUPF, SCHUPFED, GAME, YOURTURN }

// info class
data class Points( val points: Any): ServerMessage

data class Rejected(val msg: String, val orginal: PlayerMessage? = null) : ServerMessage

data class AckGameStage(val stage: Stage, val cards: List<HandCard>) : ServerMessage {

    init {
        when (stage) {
            EIGHT_CARDS -> if (cards.size != 8) throw IllegalArgumentException("Not eight cards")
            GIFT_DRAGON -> if (!cards.contains(DRG)) throw IllegalArgumentException("Dragon is not part of the cards")
            // assuming 14 cards send for all other events
            else -> if (cards.size != 14) throw IllegalArgumentException("Not 14 cards")
        }
    }
}

data class WhosTurn(val who: Player, val cards: Collection<HandCard>, val table: List<IPlayed>) :
    ServerMessage {
    val stage = GAME
}


data class Wish(val value: Int) : PlayerMessage {
    init {
        if (value < 2 || value > 14) {
            throw IllegalArgumentException("Value must be between 2 and 14, inclusive")
        }
    }
}

// id or not?
// try first without ids and go with implicit
// or jsut an event and just say tichu independently?
// todo: streamline with server req
sealed class Ack : PlayerMessage {
    class BigTichu : Ack()
    class TichuBeforeSchupf : Ack()
    class SchupfcardReceived : Ack()
    class TichuBeforePlay : Ack()
}

data class MakeYourMove(
    val handcards: List<HandCard>,
    val table: List<IPlayed>,
) : ServerMessage {
    val stage = YOURTURN
}

data class GiftDragon(val who: Player) : PlayerMessage

data class Schupf(val re: HandCard, val li: HandCard, val partner: HandCard) : PlayerMessage, ServerMessage {
    val stage = SCHUPFED
}


data class Bomb(val cards: List<PlayCard>) : PlayerMessage {
    init {
        val b = Bomb.pattern(cards)
        val sb = BombStraight.pattern(cards)
        if (b == null && sb == null) {
            throw IllegalArgumentException("no bomb")
        }
    }
}

object Tichu : PlayerMessage
object BigTichu : PlayerMessage

data class Move(val cards: Collection<PlayCard>) : PlayerMessage


/**
 *
 */
