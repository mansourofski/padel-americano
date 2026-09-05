package com.padelamericano.core.scheduler

import com.padelamericano.core.model.AmericanoRound
import com.padelamericano.core.model.Player
import com.padelamericano.core.model.ScheduleMetrics

internal object ScheduleMetricsCalculator {
    fun calculate(players: List<Player>, rounds: List<AmericanoRound>): ScheduleMetrics {
        val partnershipCounts = mutableMapOf<Pair<String, String>, Int>()
        val games = players.associate { it.id to 0 }.toMutableMap()
        val byes = players.associate { it.id to 0 }.toMutableMap()

        rounds.forEach { round ->
            round.byePlayers.forEach { player -> byes[player.id] = byes.getValue(player.id) + 1 }
            round.matches.forEach { match ->
                match.players.forEach { player -> games[player.id] = games.getValue(player.id) + 1 }
                listOf(match.teamA, match.teamB).forEach { team ->
                    val key = pairKey(team.player1.id, team.player2.id)
                    partnershipCounts[key] = partnershipCounts.getOrDefault(key, 0) + 1
                }
            }
        }

        val repeated = partnershipCounts.values.sumOf { (it - 1).coerceAtLeast(0) }
        val possiblePartnerships = players.size * (players.size - 1) / 2
        val coverage = if (possiblePartnerships == 0) 0.0
        else partnershipCounts.size * 100.0 / possiblePartnerships

        return ScheduleMetrics(
            totalRounds = rounds.size,
            totalMatches = rounds.sumOf { it.matches.size },
            repeatedPartnerships = repeated,
            partnerCoveragePercent = coverage,
            minGamesPerPlayer = games.values.minOrNull() ?: 0,
            maxGamesPerPlayer = games.values.maxOrNull() ?: 0,
            minByesPerPlayer = byes.values.minOrNull() ?: 0,
            maxByesPerPlayer = byes.values.maxOrNull() ?: 0,
        )
    }

    internal fun pairKey(a: String, b: String): Pair<String, String> = if (a <= b) a to b else b to a
}
