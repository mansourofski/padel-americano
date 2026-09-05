package com.padelamericano.app

import android.content.Context
import com.padelamericano.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Store the actual schedule so an app update cannot reshuffle saved matches. */
internal class TournamentStore(context: Context) {
    private val preferences = context.getSharedPreferences("live_tournament", Context.MODE_PRIVATE)

    fun save(state: MainUiState) {
        val root = JSONObject().put("names", state.namesText).put("courts", state.courtCount)
            .put("seed", state.seedText).put("points", state.pointsText)
        state.live?.let { live ->
            val rounds = JSONArray()
            live.schedule.rounds.forEach { round ->
                val matches = JSONArray()
                round.matches.forEach { match ->
                    val p = live.progressOf(match)
                    matches.put(JSONObject().put("number", match.matchNumber)
                        .put("players", players(match.players)).put("status", p.status.name)
                        .put("a", p.scoreA).put("b", p.scoreB))
                }
                rounds.put(JSONObject().put("number", round.number).put("matches", matches).put("byes", players(round.byePlayers)))
            }
            val m = live.schedule.metrics
            root.put("live", JSONObject().put("rounds", rounds).put("mode", live.schedule.mode.name)
                .put("courts", live.courtCount).put("points", live.pointsPerMatch)
                .put("metrics", JSONArray(listOf(m.totalRounds, m.totalMatches, m.repeatedPartnerships,
                    m.partnerCoveragePercent, m.minGamesPerPlayer, m.maxGamesPerPlayer, m.minByesPerPlayer, m.maxByesPerPlayer))))
        }
        check(preferences.edit().putString("state", root.toString()).commit())
    }

    fun load(): MainUiState {
        val root = preferences.getString("state", null)?.let(::JSONObject) ?: return MainUiState()
        val liveJson = root.optJSONObject("live")
        val live = liveJson?.let { json ->
            val progress = mutableMapOf<String, MatchProgress>()
            val roundsJson = json.getJSONArray("rounds")
            val rounds = (0 until roundsJson.length()).map { index ->
                val round = roundsJson.getJSONObject(index)
                val number = round.getInt("number")
                val matchesJson = round.getJSONArray("matches")
                val matches = (0 until matchesJson.length()).map { matchIndex ->
                    val matchJson = matchesJson.getJSONObject(matchIndex)
                    val p = readPlayers(matchJson.getJSONArray("players"))
                    val match = AmericanoMatch(number, matchJson.getInt("number"), Team(p[0], p[1]), Team(p[2], p[3]))
                    val status = MatchStatus.valueOf(matchJson.getString("status"))
                    if (status != MatchStatus.SCHEDULED) progress[match.key] = MatchProgress(status, matchJson.getInt("a"), matchJson.getInt("b"))
                    match
                }
                AmericanoRound(number, matches, readPlayers(round.getJSONArray("byes")))
            }
            val m = json.getJSONArray("metrics")
            LiveTournament(ScheduleResult(SchedulerMode.valueOf(json.getString("mode")), rounds,
                ScheduleMetrics(m.getInt(0), m.getInt(1), m.getInt(2), m.getDouble(3), m.getInt(4), m.getInt(5), m.getInt(6), m.getInt(7))),
                json.getInt("courts"), json.getInt("points"), progress)
        }
        return MainUiState(root.getString("names"), root.getInt("courts"), root.getString("seed"), root.getString("points"), live)
    }

    private fun players(players: List<Player>) = JSONArray().also { array ->
        players.forEach { array.put(JSONObject().put("id", it.id).put("name", it.displayName)) }
    }
    private fun readPlayers(array: JSONArray): List<Player> = (0 until array.length()).map {
        val player = array.getJSONObject(it)
        Player(player.getString("id"), player.getString("name"))
    }
}
