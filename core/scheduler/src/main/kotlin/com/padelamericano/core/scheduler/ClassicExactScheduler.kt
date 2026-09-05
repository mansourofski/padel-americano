package com.padelamericano.core.scheduler

import com.padelamericano.core.model.AmericanoMatch
import com.padelamericano.core.model.AmericanoRound
import com.padelamericano.core.model.Player
import com.padelamericano.core.model.ScheduleResult
import com.padelamericano.core.model.SchedulerMode
import com.padelamericano.core.model.Team
import com.padelamericano.core.model.TournamentConfig
import java.util.Collections
import java.util.Random

class ClassicExactScheduler : TournamentScheduler {

    private sealed interface Slot {
        data class Real(val player: Player) : Slot
        data object Bye : Slot
    }

    override fun generate(players: List<Player>, config: TournamentConfig): ScheduleResult {
        validatePlayers(players)
        require(players.size % 4 == 0 || players.size % 4 == 1) {
            "Exact scheduler requires N mod 4 to be 0 or 1."
        }

        val shuffled = players.toMutableList()
        Collections.shuffle(shuffled, Random(config.seed))

        val slots = shuffled.map< Player, Slot> { Slot.Real(it) }.toMutableList()
        if (players.size % 4 == 1) slots += Slot.Bye

        val pairRounds = circlePairs(slots)
        val opponentCounts = mutableMapOf<Pair<String, String>, Int>()

        val rounds = pairRounds.mapIndexed { roundIndex, pairs ->
            val byePlayers = mutableListOf<Player>()
            val teams = pairs.mapNotNull { (left, right) ->
                when {
                    left is Slot.Bye && right is Slot.Real -> {
                        byePlayers += right.player
                        null
                    }
                    right is Slot.Bye && left is Slot.Real -> {
                        byePlayers += left.player
                        null
                    }
                    left is Slot.Real && right is Slot.Real -> Team(left.player, right.player)
                    else -> null
                }
            }

            val teamMatches = pairTeamsOptimally(teams, opponentCounts)
            val matches = teamMatches.mapIndexed { matchIndex, (teamA, teamB) ->
                registerOpponents(teamA, teamB, opponentCounts)
                AmericanoMatch(
                    roundNumber = roundIndex + 1,
                    matchNumber = matchIndex + 1,
                    teamA = teamA,
                    teamB = teamB,
                )
            }

            AmericanoRound(
                number = roundIndex + 1,
                matches = matches,
                byePlayers = byePlayers,
            )
        }

        return ScheduleResult(
            mode = SchedulerMode.EXACT,
            rounds = rounds,
            metrics = ScheduleMetricsCalculator.calculate(players, rounds),
        )
    }

    private fun validatePlayers(players: List<Player>) {
        require(players.size >= 4) { "At least 4 players are required." }
        require(players.map { it.id }.distinct().size == players.size) { "Player IDs must be unique." }
    }

    private fun circlePairs(initial: List<Slot>): List<List<Pair<Slot, Slot>>> {
        require(initial.size % 2 == 0)
        val rotation = initial.toMutableList()
        val rounds = mutableListOf<List<Pair<Slot, Slot>>>()

        repeat(rotation.size - 1) {
            val pairs = buildList {
                for (i in 0 until rotation.size / 2) {
                    add(rotation[i] to rotation[rotation.lastIndex - i])
                }
            }
            rounds += pairs

            val last = rotation.removeAt(rotation.lastIndex)
            rotation.add(1, last)
        }
        return rounds
    }

    private fun pairTeamsOptimally(
        teams: List<Team>,
        opponentCounts: Map<Pair<String, String>, Int>,
    ): List<Pair<Team, Team>> {
        require(teams.size % 2 == 0)
        if (teams.isEmpty()) return emptyList()

        data class Solution(val cost: Int, val pairs: List<Pair<Int, Int>>)
        val fullMask = (1 shl teams.size) - 1
        val memo = mutableMapOf<Int, Solution>()

        fun solve(mask: Int): Solution {
            if (mask == fullMask) return Solution(0, emptyList())
            memo[mask]?.let { return it }

            val first = (teams.indices).first { mask and (1 shl it) == 0 }
            var best: Solution? = null

            for (j in first + 1 until teams.size) {
                if (mask and (1 shl j) != 0) continue
                val pairCost = opponentCost(teams[first], teams[j], opponentCounts)
                val nextMask = mask or (1 shl first) or (1 shl j)
                val tail = solve(nextMask)
                val candidate = Solution(pairCost + tail.cost, listOf(first to j) + tail.pairs)
                val currentBest = best
                if (currentBest == null || candidate.cost < currentBest.cost) best = candidate
            }
            return best!!.also { memo[mask] = it }
        }

        return solve(0).pairs.map { (a, b) -> teams[a] to teams[b] }
    }

    private fun opponentCost(
        a: Team,
        b: Team,
        counts: Map<Pair<String, String>, Int>,
    ): Int = a.players.sumOf { left ->
        b.players.sumOf { right ->
            val current = counts.getOrDefault(ScheduleMetricsCalculator.pairKey(left.id, right.id), 0)
            2 * current + 1
        }
    }

    private fun registerOpponents(
        a: Team,
        b: Team,
        counts: MutableMap<Pair<String, String>, Int>,
    ) {
        a.players.forEach { left ->
            b.players.forEach { right ->
                val key = ScheduleMetricsCalculator.pairKey(left.id, right.id)
                counts[key] = counts.getOrDefault(key, 0) + 1
            }
        }
    }
}
