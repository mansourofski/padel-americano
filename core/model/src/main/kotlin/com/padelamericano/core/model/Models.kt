package com.padelamericano.core.model

data class Player(
    val id: String,
    val displayName: String,
)

data class Team(
    val player1: Player,
    val player2: Player,
) {
    val players: List<Player> get() = listOf(player1, player2)
}

data class AmericanoMatch(
    val roundNumber: Int,
    val matchNumber: Int,
    val teamA: Team,
    val teamB: Team,
) {
    val players: List<Player> get() = teamA.players + teamB.players
}

data class AmericanoRound(
    val number: Int,
    val matches: List<AmericanoMatch>,
    val byePlayers: List<Player> = emptyList(),
)

enum class SchedulerMode {
    EXACT,
    BALANCED,
}

data class TournamentConfig(
    val seed: Long = 12_345L,
    val courtCount: Int = 1,
    val requestedRounds: Int? = null,
)

data class ScheduleMetrics(
    val totalRounds: Int,
    val totalMatches: Int,
    val repeatedPartnerships: Int,
    val partnerCoveragePercent: Double,
    val minGamesPerPlayer: Int,
    val maxGamesPerPlayer: Int,
    val minByesPerPlayer: Int,
    val maxByesPerPlayer: Int,
)

data class ScheduleResult(
    val mode: SchedulerMode,
    val rounds: List<AmericanoRound>,
    val metrics: ScheduleMetrics,
)

data class CompletedMatch(
    val match: AmericanoMatch,
    val scoreA: Int,
    val scoreB: Int,
)

data class RankingEntry(
    val player: Player,
    val points: Int,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val headToHeadDiff: Int = 0,
) {
    val diff: Int get() = pointsFor - pointsAgainst
    val averagePoints: Double get() = if (matchesPlayed == 0) 0.0 else pointsFor.toDouble() / matchesPlayed
}
