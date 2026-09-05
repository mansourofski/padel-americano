package com.padelamericano.core.scheduler

import com.padelamericano.core.model.Player
import com.padelamericano.core.model.ScheduleResult
import com.padelamericano.core.model.TournamentConfig

interface TournamentScheduler {
    fun generate(players: List<Player>, config: TournamentConfig = TournamentConfig()): ScheduleResult
}

object SchedulerFactory {
    fun forPlayerCount(playerCount: Int): TournamentScheduler {
        require(playerCount >= 4) { "At least 4 players are required." }
        return if (playerCount % 4 == 0 || playerCount % 4 == 1) {
            ClassicExactScheduler()
        } else {
            BalancedScheduler()
        }
    }
}
