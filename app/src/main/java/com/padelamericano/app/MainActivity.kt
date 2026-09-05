package com.padelamericano.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelamericano.core.model.AmericanoRound
import com.padelamericano.core.model.ScheduleResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmericanoScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmericanoScreen(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Padel Americano") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "MVP 0.1 — Générateur de planning",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.namesText,
                    onValueChange = vm::setNames,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Un joueur par ligne") },
                    minLines = 6,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::decrementCourts) { Text("− Terrain") }
                    Text("${state.courtCount} terrain(s)", modifier = Modifier.padding(top = 12.dp))
                    OutlinedButton(onClick = vm::incrementCourts) { Text("+ Terrain") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.seedText,
                    onValueChange = vm::setSeed,
                    label = { Text("Seed reproductible") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::generate, modifier = Modifier.fillMaxWidth()) {
                    Text("Générer l'Americano")
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            state.result?.let { result ->
                item { ResultSummary(result) }
                items(result.rounds, key = { it.number }) { round ->
                    RoundCard(round, state.courtCount)
                }
            }
        }
    }
}

@Composable
private fun ResultSummary(result: ScheduleResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Mode : ${result.mode}", style = MaterialTheme.typography.titleMedium)
            Text("${result.metrics.totalRounds} rounds • ${result.metrics.totalMatches} matchs")
            Text("Couverture partenaires : %.1f %%".format(result.metrics.partnerCoveragePercent))
            Text("Partenariats répétés : ${result.metrics.repeatedPartnerships}")
            Text("Matchs/joueur : ${result.metrics.minGamesPerPlayer}–${result.metrics.maxGamesPerPlayer}")
            Text("Pauses/joueur : ${result.metrics.minByesPerPlayer}–${result.metrics.maxByesPerPlayer}")
        }
    }
}

@Composable
private fun RoundCard(round: AmericanoRound, courtCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Round ${round.number}", style = MaterialTheme.typography.titleLarge)
            if (round.byePlayers.isNotEmpty()) {
                Text("Pause : ${round.byePlayers.joinToString { it.displayName }}")
            }
            round.matches.forEachIndexed { index, match ->
                val court = index % courtCount + 1
                val wave = index / courtCount + 1
                Text("Vague $wave • Terrain $court", style = MaterialTheme.typography.labelLarge)
                Text("${match.teamA.player1.displayName} / ${match.teamA.player2.displayName}")
                Text("VS")
                Text("${match.teamB.player1.displayName} / ${match.teamB.player2.displayName}")
                if (index != round.matches.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}
