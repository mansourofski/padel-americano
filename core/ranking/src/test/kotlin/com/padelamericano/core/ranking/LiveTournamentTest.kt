package com.padelamericano.core.ranking

import com.padelamericano.core.model.*
import kotlin.test.*

class LiveTournamentTest {
    private val players = (1..8).map { Player("p$it", "Joueur $it") }
    private fun match(round: Int, number: Int, offset: Int = 0) = AmericanoMatch(round, number,
        Team(players[offset], players[offset + 1]), Team(players[offset + 2], players[offset + 3]))
    private val first = match(1, 1)
    private val second = match(1, 2, 4)
    private val third = match(2, 1)
    private fun tournament(courts: Int = 2, points: Int = 32) = LiveTournament(ScheduleResult(SchedulerMode.EXACT,
        listOf(AmericanoRound(1, listOf(first, second)), AmericanoRound(2, listOf(third))),
        ScheduleMetrics(2, 3, 0, 100.0, 1, 2, 0, 0)), courts, points)

    @Test fun `match must start before scoring and cannot start twice`() {
        val initial = tournament()
        assertFailsWith<IllegalArgumentException> { initial.finish(first, 18, 14) }
        assertFailsWith<IllegalArgumentException> { initial.changePoint(first, true, 1) }
        val live = initial.start(first)
        assertEquals(MatchStatus.IN_PROGRESS, live.progressOf(first).status)
        assertFailsWith<IllegalArgumentException> { live.start(first) }
        assertFailsWith<IllegalArgumentException> { live.start(third) }
    }

    @Test fun `next round waits for every match in current wave`() {
        val live = tournament().startWave().finish(first, 18, 14)
        assertFalse(live.canStart(third))
        assertEquals(MatchStatus.IN_PROGRESS, live.progressOf(second).status)
        val next = live.finish(second, 16, 16)
        assertTrue(next.canStart(third))
        assertTrue(next.start(third).finish(third, 0, 32).isComplete)
    }

    @Test fun `one court splits round into waves`() {
        val live = tournament(courts = 1).startWave()
        assertEquals(MatchStatus.SCHEDULED, live.progressOf(second).status)
        assertFalse(live.canStart(second))
        assertTrue(live.finish(first, 32, 0).canStart(second))
    }

    @Test fun `point bounds and configurable total are enforced`() {
        val live = tournament(points = 16).start(first)
        assertFailsWith<IllegalArgumentException> { live.changePoint(first, true, -1) }
        assertFailsWith<IllegalArgumentException> { live.finish(first, 18, 14) }
        assertFailsWith<IllegalArgumentException> { live.finish(first, -1, 17) }
        val full = (1..16).fold(live) { state, _ -> state.changePoint(first, true, 1) }
        assertFailsWith<IllegalArgumentException> { full.changePoint(first, false, 1) }
        assertEquals(15, full.changePoint(first, true, -1).progressOf(first).scoreA)
        assertEquals(16, full.finish(first, 16, 0).completedMatches.single().scoreA)
    }

    @Test fun `correction replaces result and recalculates ranking without duplicates`() {
        val live = tournament().start(first).finish(first, 18, 14).finish(first, 12, 20)
        assertEquals(1, live.completedMatches.size)
        val ranking = RankingEngine.calculate(players, live.completedMatches)
        assertEquals(12, ranking.single { it.player.id == players[0].id }.points)
        assertEquals(20, ranking.single { it.player.id == players[2].id }.points)
        assertEquals(1, ranking.single { it.player.id == players[0].id }.matchesPlayed)
        assertFailsWith<IllegalArgumentException> { live.changePoint(first, true, 1) }
    }

    @Test fun `same match number in different rounds keeps separate scores`() {
        val live = tournament().startWave().finish(first, 18, 14).finish(second, 16, 16)
            .start(third).finish(third, 10, 22)
        assertEquals(18, live.progressOf(first).scoreA)
        assertEquals(10, live.progressOf(third).scoreA)
    }
}
