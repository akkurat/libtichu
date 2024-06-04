package ch.taburett.tichu.game

import ch.taburett.tichu.cards.HandCard
import ch.taburett.tichu.cards.fulldeck

data class RoundInfo(
    val prepareLog: PreparationInfo?,
    val tricks: Tricks,
    val initialCardmap: Map<Player, Collection<HandCard>>,
    val leftoverHandcards: Map<Player, Collection<HandCard>>,
) {

    val orderOfWinning = tricks.flatMap { it.playerFinished }
    val tricksByPlayer: Map<Player, List<Trick>> = tricks.groupBy { it.pointOwner }

    // tichuPoints()

    val bonusPoints: Map<PlayerGroup, Int>
        get() {
            val (first, second) = orderOfWinning
            // double win
            if (first.playerGroup == second.playerGroup) {
                return mapOf(first.playerGroup to 100, first.playerGroup.other() to 0)
            }
            return PlayerGroup.entries.associateWith { 0 }

        }

    // totalPoints()
    val cardPoints: Map<PlayerGroup, Int>
        get() {
            val cards = cards
            return cards.mapValues { (_, v) -> v.sumOf { c -> c.getPoints() } }
            // todo: well.. 100 are also possible without double win ^^
        }

    val cards: Map<PlayerGroup, List<HandCard>>
        get() {
            val (first, second) = orderOfWinning
            // double win
            if (first.playerGroup == second.playerGroup) {
                return mapOf(first.playerGroup to fulldeck, first.playerGroup.other() to listOf())
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
}