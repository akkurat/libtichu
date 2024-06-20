package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck

data class RoundInfo(
    val prepareLog: PreparationInfo?,
    val tricks: MutableTricks,
    val initialCardmap: Map<Player, Collection<HandCard>>,
    val leftoverHandcards: Map<Player, Collection<HandCard>>,
    val tichuMap: Map<Player, ETichu>,
    val name: String?,
) {
    val orderOfWinning = tricks.tricks.flatMap { it.playerFinished }
    val tricksByPlayer: Map<Player, List<Trick>> =
        Player.entries.associateWith { listOf<Trick>() } + tricks.tricks.groupBy { it.pointOwner }

    val pointsPerPlayer: Map<Player, Int>
        get() {
            return Player.entries.associateWith { totalPoints.getValue(it.playerGroup) }
        }


    val totalPoints: Map<PlayerGroup, Int> by lazy {
        cardPoints.mapValues { (k, v) ->
            v + bonusPoints.getValue(k) + tichuPoints.getValue(k)
        }
    }

    val tichuPoints: Map<PlayerGroup, Int>
        get() {
            val s =
                tichuMap.mapValues { if (orderOfWinning.indexOf(it.key) == 0) it.value.points else -it.value.points }

            return PlayerGroup.entries.associateWith { g -> g.players.sumOf { s.getValue(it) } }
        }


    val bonusPoints: Map<PlayerGroup, Int>
        get() {
            val (first, second) = orderOfWinning
            // double win
            if (first.playerGroup == second.playerGroup) {
                return mapOf(first.playerGroup to 100, first.playerGroup.other() to 0)
            }
            return PlayerGroup.entries.associateWith { 0 }

        }

    val cardPoints: Map<PlayerGroup, Int>
        get() {
            val cards = cards
            return cards.mapValues { (_, v) -> v.sumOf { c -> c.getPoints() } }
        }

    val cards: Map<PlayerGroup, List<HandCard>>
        get() {
            val (first, second) = orderOfWinning
            // double win
            if (first.playerGroup == second.playerGroup) {
                return mapOf(first.playerGroup to fulldeck.toList(), first.playerGroup.other() to listOf())
            } else {
                val third = orderOfWinning[2]
                val last = Player.entries.minus(setOf(first, second, third))[0]
                val cards: Map<PlayerGroup, MutableList<HandCard>> =
                    mapOf(PlayerGroup.A to mutableListOf(), PlayerGroup.B to mutableListOf())
                // first player keeps cards
                cards[first.playerGroup]!!.addAll(tricksByPlayer[first]!!.flatMap { it.allCards })
                // last player tricks go to winner
                tricksByPlayer[last]?.let { cards[first.playerGroup]!!.addAll(it.flatMap { it.allCards }) }
                // handcards of last player go to opposite team
                cards[last.playerGroup.other()]!!.addAll(leftoverHandcards[last]!!)

                cards[second.playerGroup]!!.addAll(tricksByPlayer[second]!!.flatMap { it.allCards })
                cards[third.playerGroup]!!.addAll(tricksByPlayer[third]!!.flatMap { it.allCards })

                cards.mapValues { (_, v) -> v.sumOf { it.getPoints() } }

                return cards;
            }
        }

    override fun toString(): String {
        return name ?: super.toString()
    }
}