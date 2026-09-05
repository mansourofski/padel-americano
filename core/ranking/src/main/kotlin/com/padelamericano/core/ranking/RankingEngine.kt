package com.padelamericano.core.ranking

import com.padelamericano.core.model.CompletedMatch
import com.padelamericano.core.model.Player
import com.padelamericano.core.model.RankingEntry

object RankingEngine {

    private data class MutableStats(
        var mp: Int = 0,
        var wins: Int = 0,
        var draws: Int = 0,
        var losses: Int = 0,
        var pf: Int = 0,
        var pa: Int = 0,
    )

    fun calculate(players: List<Player>, matches: List<CompletedMatch>): List<RankingEntry> {
        val stats = players.associate { it.id to MutableStats() }.toMutableMap()

        matches.forEach { completed ->
            require(completed.scoreA >= 0 && completed.scoreB >= 0) { "Scores cannot be negative." }
            updateTeam(
                completed.match.teamA.players,
                completed.scoreA,
                completed.scoreB,
                stats,
            )
            updateTeam(
                completed.match.teamB.players,
                completed.scoreB,
                completed.scoreA,
                stats,
            )
        }

        val base = players.map { player ->
            val s = stats.getValue(player.id)
            RankingEntry(
                player = player,
                points = s.pf,
                matchesPlayed = s.mp,
                wins = s.wins,
                draws = s.draws,
                losses = s.losses,
                pointsFor = s.pf,
                pointsAgainst = s.pa,
            )
        }

        return base
            .groupBy { it.points }
            .toSortedMap(compareByDescending { it })
            .values
            .flatMap { pointsGroup ->
                pointsGroup.groupBy { it.wins }
                    .toSortedMap(compareByDescending { it })
                    .values
                    .flatMap { winsGroup -> rankTieGroup(winsGroup, matches) }
            }
    }

    private fun rankTieGroup(entries: List<RankingEntry>, matches: List<CompletedMatch>): List<RankingEntry> {
        if (entries.size <= 1) return entries
        val ids = entries.map { it.player.id }.toSet()
        val h2h = entries.associate { it.player.id to 0 }.toMutableMap()

        matches.forEach { completed ->
            val aIds = completed.match.teamA.players.map { it.id }.toSet()
            val bIds = completed.match.teamB.players.map { it.id }.toSet()
            aIds.intersect(ids).forEach { a ->
                bIds.intersect(ids).forEach { b ->
                    h2h[a] = h2h.getValue(a) + (completed.scoreA - completed.scoreB)
                    h2h[b] = h2h.getValue(b) + (completed.scoreB - completed.scoreA)
                }
            }
        }

        return entries.map { it.copy(headToHeadDiff = h2h.getValue(it.player.id)) }
            .sortedWith(
                compareByDescending<RankingEntry> { it.headToHeadDiff }
                    .thenByDescending { it.averagePoints }
                    .thenByDescending { it.diff }
                    .thenBy { it.player.displayName.lowercase() }
            )
    }

    private fun updateTeam(
        team: List<Player>,
        scoreFor: Int,
        scoreAgainst: Int,
        stats: MutableMap<String, MutableStats>,
    ) {
        team.forEach { player ->
            val s = stats.getValue(player.id)
            s.mp++
            s.pf += scoreFor
            s.pa += scoreAgainst
            when {
                scoreFor > scoreAgainst -> s.wins++
                scoreFor < scoreAgainst -> s.losses++
                else -> s.draws++
            }
        }
    }
}
