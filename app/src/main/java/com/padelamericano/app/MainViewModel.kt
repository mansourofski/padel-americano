package com.padelamericano.app

import androidx.lifecycle.ViewModel
import com.padelamericano.core.model.Player
import com.padelamericano.core.model.ScheduleResult
import com.padelamericano.core.model.TournamentConfig
import com.padelamericano.core.scheduler.SchedulerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun setNames(value: String) = _state.update { it.copy(namesText = value, error = null) }
    fun setSeed(value: String) = _state.update { it.copy(seedText = value, error = null) }
    fun incrementCourts() = _state.update { it.copy(courtCount = (it.courtCount + 1).coerceAtMost(8)) }
    fun decrementCourts() = _state.update { it.copy(courtCount = (it.courtCount - 1).coerceAtLeast(1)) }

    fun generate() {
        runCatching {
            val names = _state.value.namesText.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
            require(names.size >= 4) { "Ajoute au moins 4 joueurs." }
            require(names.distinctBy { it.lowercase() }.size == names.size) { "Deux joueurs ont le même nom." }
            val seed = _state.value.seedText.toLongOrNull() ?: error("La seed doit être un nombre entier.")
            val players = names.mapIndexed { index, name -> Player("p-${index + 1}", name) }
            val scheduler = SchedulerFactory.forPlayerCount(players.size)
            scheduler.generate(players, TournamentConfig(seed = seed, courtCount = _state.value.courtCount))
        }.onSuccess { result ->
            _state.update { it.copy(result = result, error = null) }
        }.onFailure { error ->
            _state.update { it.copy(result = null, error = error.message ?: "Erreur inconnue") }
        }
    }
}

data class MainUiState(
    val namesText: String = "Mansour\nPhilippe\nYouness\nXavier\nAlex\nSamir\nDavid\nJulien",
    val courtCount: Int = 2,
    val seedText: String = "12345",
    val result: ScheduleResult? = null,
    val error: String? = null,
)
