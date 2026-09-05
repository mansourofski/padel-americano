package com.padelamericano.core.scheduler

import com.padelamericano.core.model.Player
import com.padelamericano.core.model.TournamentConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalancedSchedulerTest {

    @Test
    fun `6 players have equal games and equal byes`() {
        val result = BalancedScheduler().generate(players(6), TournamentConfig(seed = 10))
        assertEquals(6, result.rounds.size)
        assertEquals(4, result.metrics.minGamesPerPlayer)
        assertEquals(4, result.metrics.maxGamesPerPlayer)
        assertEquals(2, result.metrics.minByesPerPlayer)
        assertEquals(2, result.metrics.maxByesPerPlayer)
        assertValidRounds(result.rounds)
    }

    @Test
    fun `7 players have equal games and equal byes`() {
        val result = BalancedScheduler().generate(players(7), TournamentConfig(seed = 11))
        assertEquals(7, result.rounds.size)
        assertEquals(4, result.metrics.minGamesPerPlayer)
        assertEquals(4, result.metrics.maxGamesPerPlayer)
        assertEquals(3, result.metrics.minByesPerPlayer)
        assertEquals(3, result.metrics.maxByesPerPlayer)
        assertValidRounds(result.rounds)
    }

    @Test
    fun `10 players have eight games each`() {
        val result = BalancedScheduler().generate(players(10), TournamentConfig(seed = 12))
        assertEquals(8, result.metrics.minGamesPerPlayer)
        assertEquals(8, result.metrics.maxGamesPerPlayer)
        assertEquals(2, result.metrics.minByesPerPlayer)
        assertEquals(2, result.metrics.maxByesPerPlayer)
        assertValidRounds(result.rounds)
    }

    private fun players(n: Int) = (1..n).map { Player("p$it", "Player $it") }

    private fun assertValidRounds(rounds: List<com.padelamericano.core.model.AmericanoRound>) {
        rounds.forEach { round ->
            val active = round.matches.flatMap { it.players }
            assertEquals(active.size, active.map { it.id }.distinct().size)
            round.matches.forEach { assertEquals(4, it.players.map { p -> p.id }.distinct().size) }
            assertTrue(round.byePlayers.none { it.id in active.map { p -> p.id } })
        }
    }
}
