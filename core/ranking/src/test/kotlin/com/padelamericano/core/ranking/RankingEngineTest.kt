package com.padelamericano.core.ranking

import com.padelamericano.core.model.AmericanoMatch
import com.padelamericano.core.model.CompletedMatch
import com.padelamericano.core.model.Player
import com.padelamericano.core.model.Team
import kotlin.test.Test
import kotlin.test.assertEquals

class RankingEngineTest {
    @Test
    fun `points are credited individually to both teammates`() {
        val a = Player("a", "A")
        val b = Player("b", "B")
        val c = Player("c", "C")
        val d = Player("d", "D")
        val match = AmericanoMatch(1, 1, Team(a, b), Team(c, d))

        val ranking = RankingEngine.calculate(
            listOf(a, b, c, d),
            listOf(CompletedMatch(match, 18, 14)),
        )

        assertEquals(listOf("A", "B", "C", "D"), ranking.map { it.player.displayName })
        assertEquals(listOf(18, 18, 14, 14), ranking.map { it.points })
        assertEquals(1, ranking.first().wins)
        assertEquals(1, ranking.last().losses)
    }
}
