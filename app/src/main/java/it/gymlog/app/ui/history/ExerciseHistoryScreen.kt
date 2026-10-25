package it.gymlog.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.SetType
import it.gymlog.app.domain.asymmetryPct
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.design.EmptyState
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.RoundIconButton
import kotlin.math.abs

@Composable
fun ExerciseHistoryScreen(exerciseId: Long, onBack: () -> Unit, onOpenStats: () -> Unit) {
    val vm = appViewModel(key = "exhist-$exerciseId") { ExerciseHistoryViewModel(exerciseId, it.workouts, it.exercises) }
    val name by vm.name.collectAsStateWithLifecycle()
    val single by vm.single.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val c = Gym.colors
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = c.text) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    RoundIconButton(Icons.Rounded.Insights, "Grafici", onOpenStats, size = 44)
                }
            }
            Text(name, style = GymType.cardTitle, color = c.text)
            Text("${days.size} allenamenti", color = c.textSecondary)
            if (days.isEmpty()) EmptyState(Icons.Rounded.History, "Nessuna serie registrata.")
        }
        items(days, key = { it.session.id }) { d ->
            val working = d.sets.filter { it.setType != SetType.WARMUP }
            val left = working.maxOfOrNull { it.weightKg }
            val right = working.mapNotNull { it.weightKgR }.maxOrNull()
            val pct = if (single && left != null && right != null) asymmetryPct(left, right) else null
            GymCard {
                Text(formatDate(d.session.startedAt), style = MaterialTheme.typography.titleMedium, color = c.text)
                Text(d.session.title, color = c.textSecondary)
                Text(d.sets.joinToString("   ") { it.label() }, style = GymType.number.copy(fontSize = 16.sp),
                    color = c.text, modifier = Modifier.padding(top = 6.dp))
                if (pct != null) {
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SX ${formatKg(left!!)} kg", color = c.textSecondary)
                        Text("DX ${formatKg(right!!)} kg", color = c.textSecondary)
                        Text(
                            (if (pct >= 0) "+" else "") + formatKg(pct) + "%",
                            color = if (abs(pct) >= 10) c.error else c.accent,
                            style = GymType.number,
                        )
                    }
                }
                if (d.sets.any { it.notes.isNotBlank() }) {
                    Text(d.sets.filter { it.notes.isNotBlank() }.joinToString(" · ") { it.notes },
                        color = c.textSecondary, modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
