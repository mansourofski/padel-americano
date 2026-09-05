package com.padelamericano.core.scheduler

import com.padelamericano.core.model.Player
import com.padelamericano.core.model.TournamentConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassicExactSchedulerTest {

    @Test
    fun `4 players produce 3 rounds and every partnership once`() {
        val result = ClassicExactScheduler().generate(players(4), TournamentConfig(seed = 1))
        assertEquals(3, result.rounds.size)
        assertEquals(3, result.metrics.totalMatches)
        assertEquals(0, result.metrics.repeatedPartnerships)
        assertEquals(100.0, result.metrics.partnerCoveragePercent)
        assertEquals(3, result.metrics.minGamesPerPlayer)
        assertEquals(3, result.metrics.maxGamesPerPlayer)
        assertRoundInvariants(result.rounds)
    }

    @Test
    fun `5 players produce 5 rounds one bye each and every partnership once`() {
        val result = ClassicExactScheduler().generate(players(5), TournamentConfig(seed = 2))
        assertEquals(5, result.rounds.size)
        assertEquals(5, result.metrics.totalMatches)
        assertEquals(0, result.metrics.repeatedPartnerships)
        assertEquals(100.0, result.metrics.partnerCoveragePercent)
        assertEquals(4, result.metrics.minGamesPerPlayer)
        assertEquals(4, result.metrics.maxGamesPerPlayer)
        assertEquals(1, result.metrics.minByesPerPlayer)
        assertEquals(1, result.metrics.maxByesPerPlayer)
        assertRoundInvariants(result.rounds)
    }

    @Test
    fun `8 players produce 7 rounds 14 matches and seven games each`() {
        val result = ClassicExactScheduler().generate(players(8), TournamentConfig(seed = 3))
        assertEquals(7, result.rounds.size)
        assertEquals(14, result.metrics.totalMatches)
        assertEquals(0, result.metrics.repeatedPartnerships)
        assertEquals(100.0, result.metrics.partnerCoveragePercent)
        assertEquals(7, result.metrics.minGamesPerPlayer)
        assertEquals(7, result.metrics.maxGamesPerPlayer)
        assertRoundInvariants(result.rounds)
    }

    @Test
    fun `9 players produce 9 rounds 18 matches and one bye each`() {
        val result = ClassicExactScheduler().generate(players(9), TournamentConfig(seed = 4))
        assertEquals(9, result.rounds.size)
        assertEquals(18, result.metrics.totalMatches)
        assertEquals(0, result.metrics.repeatedPartnerships)
        assertEquals(100.0, result.metrics.partnerCoveragePercent)
        assertEquals(8, result.metrics.minGamesPerPlayer)
        assertEquals(8, result.metrics.maxGamesPerPlayer)
        assertEquals(1, result.metrics.minByesPerPlayer)
        assertEquals(1, result.metrics.maxByesPerPlayer)
        assertRoundInvariants(result.rounds)
    }

    @Test
    fun `same seed is deterministic`() {
        val scheduler = ClassicExactScheduler()
        val a = scheduler.generate(players(8), TournamentConfig(seed = 12345))
        val b = scheduler.generate(players(8), TournamentConfig(seed = 12345))
        assertEquals(a.rounds, b.rounds)
    }

    private fun players(n: Int) = (1..n).map { Player("p$it", "Player $it") }

    private fun assertRoundInvariants(rounds: List<com.padelamericano.core.model.AmericanoRound>) {
        rounds.forEach { round ->
            round.matches.forEach { match ->
                assertEquals(4, match.players.map { it.id }.distinct().size)
            }
            val activeIds = round.matches.flatMap { it.players }.map { it.id }
            assertEquals(activeIds.size, activeIds.distinct().size)
            assertTrue(round.byePlayers.none { it.id in activeIds })
        }
    }
}
