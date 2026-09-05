package com.padelamericano.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelamericano.core.model.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { AmericanoScreen() } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmericanoScreen(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val live = state.live
    var confirmNew by rememberSaveable { mutableStateOf(false) }
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showRanking by rememberSaveable { mutableStateOf(false) }
    val editingMatch = live?.matches?.find { it.key == editingKey }

    if (confirmNew) AlertDialog(
        onDismissRequest = { confirmNew = false },
        title = { Text("Nouveau tournoi ?") },
        text = { Text("Le planning et les scores actuels seront remplacés. Cette action est définitive.") },
        confirmButton = { TextButton(onClick = { vm.newTournament(); confirmNew = false }) { Text("Nouveau tournoi") } },
        dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Annuler") } },
    )
    if (editingMatch != null) ScoreDialog(
        editingMatch, live.progressOf(editingMatch), live.pointsPerMatch,
        onDismiss = { editingKey = null },
        onSave = { a, b -> vm.finish(editingMatch, a, b); editingKey = null },
    )

    Scaffold(topBar = { TopAppBar(title = { Text("Padel Americano") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                if (live?.hasStarted != true) {
                    Text("Préparer le tournoi", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(state.namesText, vm::setNames, Modifier.fillMaxWidth(),
                        label = { Text("Un joueur par ligne") }, minLines = 4)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = vm::decrementCourts, enabled = state.courtCount > 1) { Text("− Terrain") }
                        Text("${state.courtCount} terrain(s)")
                        TextButton(onClick = vm::incrementCourts, enabled = state.courtCount < 8) { Text("+ Terrain") }
                    }
                    OutlinedTextField(state.pointsText, vm::setPoints, Modifier.fillMaxWidth(),
                        label = { Text("Total de points par match") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(state.seedText, vm::setSeed, Modifier.fillMaxWidth(),
                        label = { Text("Seed reproductible") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::generate, modifier = Modifier.fillMaxWidth()) {
                        Text(if (live == null) "Générer l'Americano" else "Régénérer le planning")
                    }
                } else {
                    OutlinedButton(onClick = { confirmNew = true }) { Text("Nouveau tournoi") }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (live != null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(when {
                                live.isComplete -> "Tournoi terminé"
                                live.hasStarted -> "Tournoi en cours"
                                else -> "Prêt à démarrer"
                            }, style = MaterialTheme.typography.titleLarge)
                            Text("${live.completedMatches.size} / ${live.matches.size} matchs terminés • ${live.pointsPerMatch} points par match")
                            Text("${live.schedule.rounds.size} rounds • ${live.courtCount} terrain(s)")
                            if (live.currentWave.any { live.canStart(it) }) {
                                val round = live.currentWave.first().roundNumber
                                Button(onClick = vm::startWave, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (!live.hasStarted) "Démarrer les matchs" else "Démarrer la vague • Round $round")
                                }
                            }
                            if (!live.isComplete) Text("Valide tous les scores de la vague pour débloquer la suivante.")
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !showRanking, onClick = { showRanking = false }, label = { Text("Matchs") })
                        FilterChip(selected = showRanking, onClick = { showRanking = true }, label = { Text("Classement") })
                    }
                }
                if (!showRanking) items(live.schedule.rounds, key = { "round-${it.number}" }) { round ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Round ${round.number}", style = MaterialTheme.typography.titleLarge)
                            if (round.byePlayers.isNotEmpty()) Text("Pause : ${round.byePlayers.joinToString { it.displayName }}")
                            round.matches.forEachIndexed { index, match ->
                                MatchCard(match, live, index, vm, onEdit = { editingKey = match.key })
                                if (index < round.matches.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
                if (showRanking) item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (live.isComplete) "Classement final" else "Classement provisoire", style = MaterialTheme.typography.titleLarge)
                            Text("Mis à jour après chaque score validé.")
                            state.ranking.forEachIndexed { index, entry ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${index + 1}.")
                                    Text(entry.player.displayName, Modifier.weight(1f))
                                    Text("${entry.points} pts • ${entry.matchesPlayed} MJ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: AmericanoMatch, live: LiveTournament, index: Int, vm: MainViewModel, onEdit: () -> Unit) {
    val progress = live.progressOf(match)
    val total = progress.scoreA + progress.scoreB
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Vague ${index / live.courtCount + 1} • Terrain ${index % live.courtCount + 1}", style = MaterialTheme.typography.labelLarge)
        Text(when (progress.status) {
            MatchStatus.SCHEDULED -> if (live.canStart(match)) "Prêt" else "À venir"
            MatchStatus.IN_PROGRESS -> "En cours • $total / ${live.pointsPerMatch} points"
            MatchStatus.COMPLETED -> "Terminé"
        }, color = MaterialTheme.colorScheme.primary)
        TeamScore(match.teamA, progress.scoreA, progress.status, true, total < live.pointsPerMatch) { delta -> vm.changePoint(match, true, delta) }
        TeamScore(match.teamB, progress.scoreB, progress.status, false, total < live.pointsPerMatch) { delta -> vm.changePoint(match, false, delta) }
        when (progress.status) {
            MatchStatus.SCHEDULED -> Button(onClick = { vm.startMatch(match) }, enabled = live.canStart(match), modifier = Modifier.fillMaxWidth()) { Text("Démarrer le match") }
            MatchStatus.IN_PROGRESS -> {
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Saisir le score final") }
                Button(onClick = { vm.finish(match, progress.scoreA, progress.scoreB) }, enabled = total == live.pointsPerMatch,
                    modifier = Modifier.fillMaxWidth()) { Text("Valider le résultat") }
            }
            MatchStatus.COMPLETED -> TextButton(onClick = onEdit) { Text("Corriger le score") }
        }
    }
}

@Composable
private fun TeamScore(team: Team, score: Int, status: MatchStatus, teamA: Boolean, canAdd: Boolean, onPoint: (Int) -> Unit) {
    val label = if (teamA) "A" else "B"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (status == MatchStatus.SCHEDULED) "—" else score.toString(),
            modifier = Modifier.widthIn(min = 38.dp).semantics { contentDescription = "Score équipe $label : $score" },
            style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("${team.player1.displayName}\n${team.player2.displayName}", Modifier.weight(1f))
        if (status == MatchStatus.IN_PROGRESS) {
            OutlinedButton(onClick = { onPoint(-1) }, enabled = score > 0, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Retirer un point équipe $label" }) { Text("−") }
            Button(onClick = { onPoint(1) }, enabled = canAdd, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Ajouter un point équipe $label" }) { Text("+") }
        }
    }
}

@Composable
private fun ScoreDialog(match: AmericanoMatch, progress: MatchProgress, points: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var scoreA by rememberSaveable(match.key) { mutableStateOf(progress.scoreA.toString()) }
    var scoreB by rememberSaveable(match.key) { mutableStateOf(progress.scoreB.toString()) }
    val a = scoreA.toIntOrNull()
    val b = scoreB.toIntOrNull()
    val valid = a != null && b != null && a in 0..points && b in 0..points && a + b == points
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (progress.status == MatchStatus.COMPLETED) "Corriger le score" else "Score final") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${match.teamA.players.joinToString(" / ") { it.displayName }} (A)\ncontre\n${match.teamB.players.joinToString(" / ") { it.displayName }} (B)")
                OutlinedTextField(scoreA, { scoreA = it }, label = { Text("Score équipe A") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(scoreB, { scoreB = it }, label = { Text("Score équipe B") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Le total doit être égal à $points points.", color = if (valid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { if (valid) onSave(a, b) }, enabled = valid) { Text("Valider") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
