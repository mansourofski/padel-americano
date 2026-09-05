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
import kotlin.math.floor

class BalancedScheduler : TournamentScheduler {

    override fun generate(players: List<Player>, config: TournamentConfig): ScheduleResult {
        validatePlayers(players)
        require(players.size % 4 == 2 || players.size % 4 == 3) {
            "Balanced scheduler is intended for N mod 4 = 2 or 3."
        }

        val roundCount = config.requestedRounds ?: players.size
        val restarts = if (players.size <= 12) 30 else 15
        var best: ScheduleResult? = null
        var bestCost = Long.MAX_VALUE

        repeat(restarts) { restart ->
            val seed = config.seed + restart * 1_000_003L
            val rounds = buildCandidateSchedule(players, roundCount, seed)
            val metrics = ScheduleMetricsCalculator.calculate(players, rounds)
            val cost = scheduleCost(rounds, metrics.repeatedPartnerships)
            if (cost < bestCost) {
                bestCost = cost
                best = ScheduleResult(
                    mode = SchedulerMode.BALANCED,
                    rounds = rounds,
                    metrics = metrics,
                )
            }
        }

        return best!!
    }

    private fun validatePlayers(players: List<Player>) {
        require(players.size >= 4) { "At least 4 players are required." }
        require(players.map { it.id }.distinct().size == players.size) { "Player IDs must be unique." }
    }

    private fun buildCandidateSchedule(
        players: List<Player>,
        roundCount: Int,
        seed: Long,
    ): List<AmericanoRound> {
        val ordered = players.toMutableList()
        Collections.shuffle(ordered, Random(seed))

        val partnerCounts = mutableMapOf<Pair<String, String>, Int>()
        val opponentCounts = mutableMapOf<Pair<String, String>, Int>()
        val remainder = players.size % 4
        val offsets = (0 until remainder).map { k -> floor(k * players.size.toDouble() / remainder).toInt() }
        val rounds = mutableListOf<AmericanoRound>()
        val rng = Random(seed xor 0x5DEECE66DL)

        repeat(roundCount) { roundIndex ->
            val byeSet = if (roundCount == players.size) {
                offsets.map { offset -> ordered[(roundIndex + offset) % ordered.size] }.toSet()
            } else {
                chooseGreedyByes(ordered, rounds, remainder, rng)
            }
            val active = ordered.filterNot { it in byeSet }
            require(active.size % 4 == 0)

            val groups = bestRoundGrouping(active, partnerCounts, opponentCounts, rng)
            val matches = groups.mapIndexed { matchIndex, split ->
                registerPartnership(split.first, partnerCounts)
                registerPartnership(split.second, partnerCounts)
                registerOpponents(split.first, split.second, opponentCounts)
                AmericanoMatch(
                    roundNumber = roundIndex + 1,
                    matchNumber = matchIndex + 1,
                    teamA = split.first,
                    teamB = split.second,
                )
            }
            rounds += AmericanoRound(roundIndex + 1, matches, byeSet.toList())
        }
        return rounds
    }

    private fun chooseGreedyByes(
        players: List<Player>,
        rounds: List<AmericanoRound>,
        count: Int,
        rng: Random,
    ): Set<Player> {
        val byeCounts = players.associateWith { p -> rounds.count { r -> p in r.byePlayers } }
        val previous = rounds.lastOrNull()?.byePlayers?.toSet().orEmpty()
        return players.shuffled(rng).sortedWith(
            compareBy<Player> { byeCounts.getValue(it) }
                .thenBy { if (it in previous) 1 else 0 }
        ).take(count).toSet()
    }

    private fun bestRoundGrouping(
        active: List<Player>,
        partnerCounts: Map<Pair<String, String>, Int>,
        opponentCounts: Map<Pair<String, String>, Int>,
        rng: Random,
    ): List<Pair<Team, Team>> {
        val attempts = if (active.size <= 16) 500 else 250
        var bestCost = Long.MAX_VALUE
        var best: List<Pair<Team, Team>>? = null

        repeat(attempts) {
            val shuffled = active.toMutableList()
            Collections.shuffle(shuffled, rng)
            var cost = 0L
            val splits = mutableListOf<Pair<Team, Team>>()

            shuffled.chunked(4).forEach { group ->
                val options = listOf(
                    Team(group[0], group[1]) to Team(group[2], group[3]),
                    Team(group[0], group[2]) to Team(group[1], group[3]),
                    Team(group[0], group[3]) to Team(group[1], group[2]),
                )
                val selected = options.minBy { splitCost(it.first, it.second, partnerCounts, opponentCounts) }
                cost += splitCost(selected.first, selected.second, partnerCounts, opponentCounts)
                splits += selected
            }

            if (cost < bestCost) {
                bestCost = cost
                best = splits
            }
        }
        return best!!
    }

    private fun splitCost(
        a: Team,
        b: Team,
        partnerCounts: Map<Pair<String, String>, Int>,
        opponentCounts: Map<Pair<String, String>, Int>,
    ): Long {
        val partnerA = partnerCounts.getOrDefault(ScheduleMetricsCalculator.pairKey(a.player1.id, a.player2.id), 0)
        val partnerB = partnerCounts.getOrDefault(ScheduleMetricsCalculator.pairKey(b.player1.id, b.player2.id), 0)
        val partnerPenalty = (partnerA + partnerB) * 100_000L
        val opponentPenalty = a.players.sumOf { left ->
            b.players.sumOf { right ->
                opponentCounts.getOrDefault(ScheduleMetricsCalculator.pairKey(left.id, right.id), 0).toLong()
            }
        } * 100L
        return partnerPenalty + opponentPenalty
    }

    private fun registerPartnership(team: Team, counts: MutableMap<Pair<String, String>, Int>) {
        val key = ScheduleMetricsCalculator.pairKey(team.player1.id, team.player2.id)
        counts[key] = counts.getOrDefault(key, 0) + 1
    }

    private fun registerOpponents(a: Team, b: Team, counts: MutableMap<Pair<String, String>, Int>) {
        a.players.forEach { left ->
            b.players.forEach { right ->
                val key = ScheduleMetricsCalculator.pairKey(left.id, right.id)
                counts[key] = counts.getOrDefault(key, 0) + 1
            }
        }
    }

    private fun scheduleCost(rounds: List<AmericanoRound>, repeatedPartnerships: Int): Long {
        val opponentCounts = mutableMapOf<Pair<String, String>, Int>()
        rounds.flatMap { it.matches }.forEach { match ->
            match.teamA.players.forEach { left ->
                match.teamB.players.forEach { right ->
                    val key = ScheduleMetricsCalculator.pairKey(left.id, right.id)
                    opponentCounts[key] = opponentCounts.getOrDefault(key, 0) + 1
                }
            }
        }
        val opponentRepeat = opponentCounts.values.sumOf { (it - 1).coerceAtLeast(0) }
        return repeatedPartnerships * 1_000_000L + opponentRepeat * 100L
    }
}
