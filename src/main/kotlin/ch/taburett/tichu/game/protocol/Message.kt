package ch.taburett.tichu.game.protocol

import ch.taburett.tichu.cards.DRG
import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.PlayCard
import ch.taburett.tichu.game.core.ETichu
import ch.taburett.tichu.game.core.Player
import ch.taburett.tichu.game.core.TichuTable
import ch.taburett.tichu.game.gamelog.Trick
import ch.taburett.tichu.game.gamelog.Tricks
import ch.taburett.tichu.game.protocol.Message.Move
import ch.taburett.tichu.game.protocol.Message.Stage.*
import ch.taburett.tichu.patterns.BombStraight
import ch.taburett.tichu.patterns.TichuPattern

interface Message {

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
    enum class Stage { EIGHT_CARDS, PRE_SCHUPF, POST_SCHUPF, GIFT_DRAGON, SCHUPF, SCHUPFED, OTHERS_TURN, YOURTURN }

    // info class
    data class Points(val points: Any) : ServerMessage

    data class Rejected(val msg: String, val orginal: Any? = null) : ServerMessage

    data class AckGameStage(
        override val youAre: Player,
        val stage: Stage,
        override val handcards: List<HandCard>,
        override val tichuMap: Map<Player, ETichu>
    ) :
        ServerMessage, CardsMessage{

        init {
            when (stage) {
                EIGHT_CARDS -> if (handcards.size != 8) throw IllegalArgumentException("Not eight cards")
                GIFT_DRAGON -> if (!handcards.contains(DRG)) throw IllegalArgumentException("Dragon is not part of the cards")
                // assuming 14 cards send for all other events
                else -> if (handcards.size != 14) throw IllegalArgumentException("Not 14 cards")
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

    // todo merge with WhosTurn and make YOU a flag
// all the
    data class WhosMove(
        override val youAre: Player,
        val who: Player,
        override val handcards: List<HandCard>,
        val table: TichuTable,
        val last: Trick?,
        val tricks: Tricks,
        val wish: Int? = null,
        val dragonGift: Boolean = false,
        val cardCounts: Map<Player, Int>,
        override val tichuMap: Map<Player, ETichu>,
        val goneCards: Set<PlayCard>,
    ) : ServerMessage, CardsMessage {
        val yourMove = this.youAre == who
        val stage = if (yourMove) {
            if (dragonGift) {
                GIFT_DRAGON
            } else {
                YOURTURN
            }
        } else {
            OTHERS_TURN
        }

        override fun toString(): String = "$stage|$table|${handcards.joinToString()}"
    }

    data class GiftDragon(val to: ReLi) : PlayerMessage {
        enum class ReLi {
            RE {
                override fun map(u: Player): Player = u.re
            },
            LI {
                override fun map(u: Player): Player = u.li
            };

            abstract fun map(u: Player): Player
        }
    }

    data class Schupf(val re: HandCard, val li: HandCard, val partner: HandCard) : PlayerMessage, ServerMessage {
        val stage = SCHUPFED
    }

    data class Bomb(val cards: List<PlayCard>) : PlayerMessage {
        val pattern: TichuPattern

        init {
            val b = ch.taburett.tichu.patterns.Bomb.pattern(cards)
            val sb = BombStraight.pattern(cards)
            if (b != null) {
                pattern = b;
            } else if (sb != null) {
                pattern = sb
            } else {
                throw IllegalArgumentException("no bomb")
            }
        }
    }

    class Announce {
        class SmallTichu : PlayerMessage
        class BigTichu : PlayerMessage
    }

    data class Move(val cards: MutableCollection<out PlayCard>, val wish: Int? = null) : PlayerMessage {
        constructor(cards: Collection<PlayCard>) : this(cards.toMutableList())
//    constructor(card: PlayCard?, wish: Int? = null) : this(if (card != null) listOf(card) else listOf(), wish)
    }


    /**
     *
     */

}


interface CardsMessage: Message.ServerMessage {
    val youAre: Player
    val handcards: List<HandCard>
    val tichuMap: Map<Player, ETichu>

}

fun createMove(cards: Collection<PlayCard>, wish: Int? = null) = Move(cards.toMutableList(), wish)

fun moveSingle(card: PlayCard?, wish: Int? = null) =
    Move(if (card != null) mutableListOf(card) else mutableListOf(), wish)
