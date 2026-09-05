package com.padelamericano.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.padelamericano.core.model.*
import com.padelamericano.core.ranking.RankingEngine
import com.padelamericano.core.scheduler.SchedulerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TournamentStore(application)
    private val _state = MutableStateFlow(runCatching { store.load() }.getOrElse {
        MainUiState(error = "Le tournoi sauvegardé n'a pas pu être chargé.")
    })
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private fun configure(change: (MainUiState) -> MainUiState) {
        if (_state.value.live?.hasStarted == true) return
        save(change(_state.value).copy(live = null, error = null))
    }
    fun setNames(value: String) = configure { it.copy(namesText = value) }
    fun setSeed(value: String) = configure { it.copy(seedText = value) }
    fun setPoints(value: String) = configure { it.copy(pointsText = value) }
    fun incrementCourts() = configure { it.copy(courtCount = (it.courtCount + 1).coerceAtMost(8)) }
    fun decrementCourts() = configure { it.copy(courtCount = (it.courtCount - 1).coerceAtLeast(1)) }

    fun generate() {
        if (_state.value.live?.hasStarted == true) return
        runCatching {
            val state = _state.value
            val names = state.namesText.lines().map { it.trim() }.filter { it.isNotBlank() }
            require(names.size >= 4) { "Ajoute au moins 4 joueurs." }
            require(names.distinctBy { it.lowercase() }.size == names.size) { "Deux joueurs ont le même nom." }
            val seed = state.seedText.toLongOrNull() ?: error("La seed doit être un nombre entier.")
            val points = state.pointsText.toIntOrNull() ?: error("Indique un nombre de points entier.")
            require(points in 1..999) { "Choisis entre 1 et 999 points." }
            val players = names.mapIndexed { index, name -> Player("p-${index + 1}", name) }
            LiveTournament(SchedulerFactory.forPlayerCount(players.size).generate(
                players, TournamentConfig(seed = seed, courtCount = state.courtCount)
            ), state.courtCount, points)
        }.onSuccess { live -> save(_state.value.copy(live = live, error = null)) }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Erreur inconnue") } }
    }

    private fun save(next: MainUiState) {
        runCatching { store.save(next) }
            .onSuccess { _state.value = next }
            .onFailure { _state.update { it.copy(error = "Sauvegarde impossible. Réessaie avant de quitter.") } }
    }
    private fun change(action: (LiveTournament) -> LiveTournament) {
        val live = _state.value.live ?: return
        runCatching { action(live) }
            .onSuccess { save(_state.value.copy(live = it, error = null)) }
            .onFailure { error -> _state.update { it.copy(error = error.message) } }
    }
    fun startWave() = change { it.startWave() }
    fun startMatch(match: AmericanoMatch) = change { it.start(match) }
    fun changePoint(match: AmericanoMatch, teamA: Boolean, delta: Int) = change { it.changePoint(match, teamA, delta) }
    fun finish(match: AmericanoMatch, scoreA: Int, scoreB: Int) = change { it.finish(match, scoreA, scoreB) }
    fun newTournament() = save(_state.value.copy(live = null, error = null))
}

data class MainUiState(
    val namesText: String = "Mansour\nPhilippe\nYouness\nXavier\nAlex\nSamir\nDavid\nJulien",
    val courtCount: Int = 2,
    val seedText: String = "12345",
    val pointsText: String = "32",
    val live: LiveTournament? = null,
    val error: String? = null,
) {
    val ranking get() = live?.let { tournament ->
        val players = tournament.schedule.rounds.flatMap { round -> round.matches.flatMap { it.players } + round.byePlayers }.distinctBy { it.id }
        RankingEngine.calculate(players, tournament.completedMatches)
    }.orEmpty()
}
