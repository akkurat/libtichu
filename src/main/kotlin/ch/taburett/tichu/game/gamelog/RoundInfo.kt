package ch.taburett.tichu.game.gamelog

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck
import ch.taburett.tichu.game.core.preparation.PreparationInfo
import ch.taburett.tichu.game.core.common.ETichu
import ch.taburett.tichu.game.core.common.EPlayer
import ch.taburett.tichu.game.core.common.EPlayerGroup

data class RoundInfo(
    val prepareLog: PreparationInfo?,
    val tricks: MutableTricks,
    val initialCardmap: Map<EPlayer, Collection<HandCard>>,
    val leftoverHandcards: Map<EPlayer, Collection<HandCard>>,
    val tichuMap: Map<EPlayer, ETichu>,
    val name: String?,
) {
    val orderOfWinning = tricks.tricks.flatMap { it.playerFinishedEntry }
    val tricksByPlayer: Map<EPlayer, List<Trick>> by lazy {
        EPlayer.entries.associateWith { listOf<Trick>() } + tricks.tricks.groupBy { it.pointOwner }
    }

    val pointsPerPlayer: Map<EPlayer, Int> by lazy {
        EPlayer.entries.associateWith { totalPoints.getValue(it.playerGroup) }
    }


    val totalPoints: Map<EPlayerGroup, Int> by lazy {
        cardPoints.mapValues { (k, v) ->
            v + bonusPoints.getValue(k) + tichuPoints.getValue(k)
        }
    }

    val tichuPoints: Map<EPlayerGroup, Int> by lazy {
        val s = tichuPointsPerPlayer
        EPlayerGroup.entries.associateWith { g -> g.players.sumOf { s.getValue(it) } }
    }

    val tichuPointsPerPlayer: Map<EPlayer, Int> by lazy {
        tichuMap.mapValues { if (orderOfWinning.indexOf(it.key) == 0) it.value.points else -it.value.points }
    }


    val bonusPoints: Map<EPlayerGroup, Int> by lazy {
        val (first, second) = orderOfWinning
        // double win
        if (first.playerGroup == second.playerGroup) {
            mapOf(first.playerGroup to 100, first.playerGroup.other() to 0)
        } else {
            EPlayerGroup.entries.associateWith { 0 }
        }
    }

    val cardPoints: Map<EPlayerGroup, Int> by lazy {
        val cards = cards
        cards.mapValues { (_, v) -> v.sumOf { c -> c.getPoints() } }
    }

    val cards: Map<EPlayerGroup, List<HandCard>>
        get() {
            val (first, second) = orderOfWinning
            // double win
            if (first.playerGroup == second.playerGroup) {
                return mapOf(first.playerGroup to fulldeck.toList(), first.playerGroup.other() to listOf())
            } else {
                val third = orderOfWinning[2]
                val last = EPlayer.entries.minus(setOf(first, second, third))[0]
                val cards: Map<EPlayerGroup, MutableList<HandCard>> =
                    mapOf(EPlayerGroup.A to mutableListOf(), EPlayerGroup.B to mutableListOf())
                // first player keeps cards
                cards[first.playerGroup]!!.addAll(tricksByPlayer[first]!!.flatMap { it.allCards })
                // last player tricks go to winner
                tricksByPlayer[last]?.let { cards[first.playerGroup]!!.addAll(it.flatMap { it.allCards }) }
                // handcards of last player go to opposite team
                cards[last.playerGroup.other()]!!.addAll(leftoverHandcards[last]!!)

                cards[second.playerGroup]!!.addAll(tricksByPlayer[second]!!.flatMap { it.allCards })
                cards[third.playerGroup]!!.addAll(tricksByPlayer[third]!!.flatMap { it.allCards })
//                cards.mapValues { (_, v) -> v.sumOf { it.getPoints() } }

                return cards;
            }
        }

    override fun toString(): String {
        return name ?: super.toString()
    }
}