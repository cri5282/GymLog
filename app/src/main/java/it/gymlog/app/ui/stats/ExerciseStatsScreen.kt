package it.gymlog.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.domain.ExerciseProgress
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.design.EmptyState
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.SectionLabel

@Composable
fun ExerciseStatsScreen(exerciseId: Long, onBack: () -> Unit, onOpenHistory: () -> Unit) {
    val vm = appViewModel(key = "exstats-$exerciseId") { ExerciseStatsViewModel(exerciseId, it.workouts, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Gym.colors
    val progress = state.progress
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
                Spacer(Modifier.weight(1f))
                RoundIconButton(Icons.Rounded.History, "Storico completo", onOpenHistory, size = 44)
            }
            Text(progress?.name ?: "Esercizio", style = GymType.screenTitle, color = c.text)
            Text(progress?.muscle?.label.orEmpty(), color = c.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            if (progress == null) EmptyState(Icons.Rounded.History, "Nessuna serie registrata per questo esercizio.")
        }
        if (progress != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecordTile("Record", "${formatKg(progress.bestKg)} kg", Modifier.weight(1f))
                    RecordTile("1RM stimato", "${formatKg(progress.bestE1rm)} kg", Modifier.weight(1f))
                }
            }
            item { MetricCard("Carico massimo", progress.points.map { it.bestKg }, "kg") }
            item { MetricCard("1RM stimato", progress.points.map { it.e1rmKg }, "kg") }
            item { MetricCard("Volume per sessione", progress.points.map { it.volumeKg }, "kg") }
            if (progress.single) {
                item {
                    SectionLabel("Sinistro e destro")
                    val last = state.asymmetry.lastOrNull()
                    if (last == null) {
                        GymCard { Text("Registra i carichi dei due lati per vedere il divario.", color = c.textSecondary) }
                    } else {
                        GymCard {
                            Text("Ultima sessione", color = c.textSecondary)
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("SX ${formatKg(last.leftKg)} kg", style = GymType.number, color = c.text)
                                Text("DX ${formatKg(last.rightKg)} kg", style = GymType.number, color = c.text)
                                Text(signedPct(last.pct), style = GymType.number,
                                    color = if (kotlin.math.abs(last.pct) >= 10) c.error else c.accent)
                            }
                            Text(if (last.pct < 0) "Il lato destro è più debole." else "Il lato destro è più forte.",
                                color = c.textSecondary, modifier = Modifier.padding(top = 4.dp))
                            Sparkline(state.asymmetry.map { it.pct }, Modifier.fillMaxWidth().height(72.dp).padding(top = 12.dp),
                                color = c.accent)
                        }
                    }
                }
                items(state.asymmetry.reversed().take(8), key = { it.date }) { p ->
                    GymCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDate(p.date), color = c.text, modifier = Modifier.weight(1f))
                            Text("SX ${formatKg(p.leftKg)} / DX ${formatKg(p.rightKg)}", color = c.textSecondary)
                            Text(signedPct(p.pct), style = GymType.number, color = c.text,
                                modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Gym.colors
    GymCard(modifier) {
        Text(label, color = c.textSecondary)
        Text(value, style = GymType.stat, color = c.text, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MetricCard(title: String, values: List<Double>, unit: String) {
    val c = Gym.colors
    GymCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text("${formatKg(values.lastOrNull() ?: 0.0)} $unit", style = GymType.number, color = c.accent)
        }
        Sparkline(values, Modifier.fillMaxWidth().height(90.dp).padding(top = 12.dp))
        if (values.isNotEmpty()) {
            Row(Modifier.padding(top = 4.dp)) {
                Text("min ${formatKg(values.min())}", style = GymType.label, color = c.textSecondary)
                Spacer(Modifier.weight(1f))
                Text("max ${formatKg(values.max())}", style = GymType.label, color = c.textSecondary)
            }
        }
    }
}

private fun signedPct(v: Double): String = (if (v >= 0) "+" else "") + formatKg(v) + "%"
