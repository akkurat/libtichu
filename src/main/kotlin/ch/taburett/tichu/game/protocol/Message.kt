package ch.taburett.tichu.game.protocol

import ch.taburett.tichu.cards.DRG
import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.NumberCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.Player
import ch.taburett.tichu.game.Table
import ch.taburett.tichu.game.Trick
import ch.taburett.tichu.game.protocol.Stage.*
import ch.taburett.tichu.patterns.Bomb
import ch.taburett.tichu.patterns.BombStraight

interface Message

interface ServerMessage : Message {
}

sealed interface PlayerMessage : Message

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
data class Points(val points: Any) : ServerMessage

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

data class WhosTurn(val who: Player, val cards: Collection<HandCard>, val table: Table, val last: Trick?) :
    ServerMessage {
    val stage = GAME
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
    val table: Table,
    val last: Trick?,
    val wish: Int? = null,
    val dragonGift: Boolean = false,
) : ServerMessage {
    // todo: this is wrong...
    fun mightFullFillWish(): Boolean = handcards.filterIsInstance<NumberCard>().any { it.getValue() == wish }
    val stage = if (dragonGift ) GIFT_DRAGON else YOURTURN
}

data class GiftDragon(val to: ReLi) : PlayerMessage {
    enum class ReLi {
        RE {
            override fun map(u: Player): Player = u.re()
        },
        LI {
            override fun map(u: Player): Player = u.li()
        };

        abstract fun map(u: Player): Player
    }
}

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

data class Move(val cards: MutableCollection<out PlayCard>, val wish: Int? = null) : PlayerMessage {
    constructor(cards: Collection<PlayCard>) : this(cards.toMutableList(), null)
}


/**
 *
 */
