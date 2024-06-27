package ch.taburett.tichu.game.core.gameplay

import ch.taburett.tichu.cards.*
import ch.taburett.tichu.game.core.common.IDeck
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.playerList

class MutableDeck private constructor(
    _cardMap: Map<EPlayer, Collection<HandCard>>,
    override val initialPlayer: EPlayer,
    _goneCards: Collection<PlayCard>,
) : IDeck {
    companion object {

        fun createInitial(cardMap: Map<EPlayer, Collection<HandCard>>): MutableDeck {
            // logic later on works only if all cards in map are present
            val valid = fulldeckSet == cardMap.values.flatten().toSet()
            if (!valid) {
                throw IllegalArgumentException("Card map must contain complete deck. use createStarted for a partial deck [requires start player]")
            }
            val initialPlayer = cardMap
                .filterValues { it.contains(MAH) }
                .map { it.key }
                .first()
            return MutableDeck(cardMap, initialPlayer, emptySet())
        }

        fun createStarted(
            cardMap: Map<EPlayer, Collection<HandCard>>,
            initialPlayer: EPlayer,
            _goneCards: Collection<PlayCard>,
        ): MutableDeck {
            val deckVAlid = (cardMap.values.flatten() + _goneCards.map { it.asHandcard() }).toSet() == fulldeckSet
            if (deckVAlid) {
                return MutableDeck(cardMap, initialPlayer, _goneCards)
            } else {
                throw IllegalArgumentException("Card map and gone cards must add up to full deck")
            }
        }

        fun copy(deck: IDeck): MutableDeck {
            return if (deck is MutableDeck) {
                deck.copy()
            } else {
                MutableDeck(deck.getCardMap(), deck.initialPlayer, deck.goneCards())
            }
        }

        fun createInitial(): MutableDeck {
            return createInitial(EPlayer.entries.zip(fulldeck.shuffled().chunked(14)).toMap())
        }
    }

    private val cardMap: Map<EPlayer, MutableList<HandCard>> = _cardMap.mapValues { (_, l) -> l.toMutableList() }
    private val _goneCards = _goneCards.toMutableSet() ?: mutableSetOf()


    override fun activePlayers(): Set<EPlayer> {
        return cardMap.filter { (p, v) -> v.isNotEmpty() }.keys
    }

    override fun finishedPlayers(): Set<EPlayer> {
        return cardMap.filter { (p, v) -> v.isEmpty() }.keys
    }

    override tailrec fun nextPlayer(lastPlayer: EPlayer, step: Int, cnt: Int): EPlayer {
        if (cnt >= 3) {
            throw IllegalStateException("game should have ended already")
        }
        val nextIdx = ((playerList.indexOf(lastPlayer)) + step) % playerList.size
        val player = playerList[nextIdx]
        return if (cardMap.getValue(player).isEmpty()) {
            nextPlayer(player, 1, cnt + 1)
        } else {
            player
        }
    }

    override fun deckSizes() = cardMap.mapValues { it.value.size }
    override fun cards(player: EPlayer): List<HandCard> = cardMap.getValue(player).toList()
    override fun getCardMap() = cardMap.mapValues { (_, v) -> v.toList() }

    override fun roundEnded(): Boolean {
        val size = finishedPlayers().size
        val s = when (size) {
            2 -> {
                finishedPlayers().first().playerGroup == finishedPlayers().last().playerGroup
            }

            3 -> {
                true
            }

            4 -> {
//                throw IllegalArgumentException("game was already finished ")
                true
            }

            else -> {
                false
            }
        }
//        if(s) {
//            println("$size $this")
//        }
        return s
    }

    fun playCards(player: EPlayer, playedCards: Collection<PlayCard>) {
        cardMap.getValue(player).removeAll(playedCards.map { it.asHandcard() })
        _goneCards.addAll(playedCards)
    }

    override fun leftovers(): Map<EPlayer, List<HandCard>> = cardMap.mapValues { (_, v) -> v.toList() }
    override fun goneCards(): Set<PlayCard> = _goneCards
    override fun copy(): MutableDeck {
        return MutableDeck(cardMap, initialPlayer, _goneCards)
    }
}
