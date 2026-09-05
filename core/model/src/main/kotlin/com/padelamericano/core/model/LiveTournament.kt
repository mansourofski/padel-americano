package com.padelamericano.core.model

enum class MatchStatus { SCHEDULED, IN_PROGRESS, COMPLETED }

data class MatchProgress(
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
)

val AmericanoMatch.key: String get() = "$roundNumber:$matchNumber"

/** Immutable tournament state; only the earliest unfinished wave can start. */
data class LiveTournament(
    val schedule: ScheduleResult,
    val courtCount: Int,
    val pointsPerMatch: Int = 32,
    val progress: Map<String, MatchProgress> = emptyMap(),
) {
    init {
        require(courtCount > 0)
        require(pointsPerMatch in 1..999)
    }

    val matches: List<AmericanoMatch> get() = schedule.rounds.flatMap { it.matches }
    fun progressOf(match: AmericanoMatch): MatchProgress = progress[match.key] ?: MatchProgress()
    val completedMatches: List<CompletedMatch> get() = matches.mapNotNull { match ->
        progressOf(match).takeIf { it.status == MatchStatus.COMPLETED }
            ?.let { CompletedMatch(match, it.scoreA, it.scoreB) }
    }
    val isComplete: Boolean get() = matches.isNotEmpty() && completedMatches.size == matches.size
    val hasStarted: Boolean get() = progress.isNotEmpty()
    val currentWave: List<AmericanoMatch> get() = schedule.rounds
        .flatMap { it.matches.chunked(courtCount) }
        .firstOrNull { wave -> wave.any { progressOf(it).status != MatchStatus.COMPLETED } }
        .orEmpty()

    fun canStart(match: AmericanoMatch): Boolean = match in currentWave &&
        progressOf(match).status == MatchStatus.SCHEDULED

    fun start(match: AmericanoMatch): LiveTournament {
        require(canStart(match)) { "Termine la vague précédente avant de démarrer ce match." }
        return copy(progress = progress + (match.key to MatchProgress(MatchStatus.IN_PROGRESS)))
    }

    fun startWave(): LiveTournament = currentWave.filter(::canStart).fold(this) { live, match -> live.start(match) }

    fun changePoint(match: AmericanoMatch, teamA: Boolean, delta: Int): LiveTournament {
        require(match in matches)
        val previous = progressOf(match)
        require(previous.status == MatchStatus.IN_PROGRESS) { "Démarre le match avant de saisir le score." }
        require(delta == -1 || delta == 1)
        val next = if (teamA) previous.copy(scoreA = previous.scoreA + delta)
            else previous.copy(scoreB = previous.scoreB + delta)
        require(next.scoreA >= 0 && next.scoreB >= 0 && next.scoreA + next.scoreB <= pointsPerMatch) {
            "Le total ne peut pas dépasser $pointsPerMatch points."
        }
        return copy(progress = progress + (match.key to next))
    }

    fun finish(match: AmericanoMatch, scoreA: Int, scoreB: Int): LiveTournament {
        require(match in matches)
        require(progressOf(match).status != MatchStatus.SCHEDULED) { "Démarre d'abord le match." }
        require(scoreA in 0..pointsPerMatch && scoreB in 0..pointsPerMatch && scoreA + scoreB == pointsPerMatch) {
            "Le total doit être égal à $pointsPerMatch points."
        }
        return copy(progress = progress + (match.key to MatchProgress(MatchStatus.COMPLETED, scoreA, scoreB)))
    }
}
