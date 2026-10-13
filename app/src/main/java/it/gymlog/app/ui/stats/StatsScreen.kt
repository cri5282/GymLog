package it.gymlog.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.SessionSource
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.design.EmptyState
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymChip
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.design.SectionLabel
import it.gymlog.app.ui.design.StatTile
import it.gymlog.app.ui.home.minutes

@Composable
fun StatsScreen(onOpenHistory: () -> Unit, onOpenSession: (Long) -> Unit) {
    val vm = appViewModel { StatsViewModel(it.workouts) }
    LifecycleResumeEffect(Unit) { vm.refreshDay(); onPauseOrDispose {} }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = Gym.colors
    if (s.loading) return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Statistiche", "Progressi e storico") { RoundIconButton(Icons.Rounded.History, "Storico", onOpenHistory) }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Rounded.FitnessCenter, "Allenamenti", "${s.total}", Modifier.weight(1f))
                StatTile(Icons.Rounded.CalendarToday, "Questo mese", "${s.thisMonth}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Rounded.LocalFireDepartment, "Settimane di fila", "${s.streak}", Modifier.weight(1f))
                StatTile(Icons.Rounded.Repeat, "Serie questa sett.", "${s.setsThisWeek}", Modifier.weight(1f), valueColor = c.accent)
            }
            GymCard {
                Text("Attività – ultimi 12 mesi", color = c.textSecondary, modifier = Modifier.padding(bottom = 10.dp))
                ActivityHeatmap(s.levels, s.today)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Allenamenti recenti", Modifier.weight(1f))
                if (s.total > 0) TextButton(onClick = onOpenHistory) { Text("Vedi tutti", color = c.accent) }
            }
            if (s.recent.isEmpty()) EmptyState(Icons.Rounded.History, "Nessun allenamento. Puoi importare lo storico da Hevy nelle Impostazioni.")
            s.recent.forEach { SessionCard(it) { onOpenSession(it.id) } }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Card di una sessione conclusa: titolo, data, durata, serie. */
@Composable
fun SessionCard(session: SessionTime, onClick: () -> Unit) {
    val c = Gym.colors
    GymCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                    if (session.source == SessionSource.HEVY_IMPORT) GymChip("Hevy")
                }
                Text(buildString {
                    append(formatDate(session.startedAt))
                    session.minutes()?.takeIf { it > 0 }?.let { append(" · $it min") }
                    append(" · ${session.setCount} serie")
                }, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
        }
    }
}
