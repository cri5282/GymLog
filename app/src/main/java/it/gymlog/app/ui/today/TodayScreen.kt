package it.gymlog.app.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.ui.common.ScreenTitle
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatTime
import it.gymlog.app.ui.common.weekdayLabel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(onOpenWorkout: (Long) -> Unit) {
    val vm = appViewModel { TodayViewModel(it.programs, it.workouts) }
    val s by vm.state.collectAsStateWithLifecycle()
    val todayName = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN).replaceFirstChar { it.uppercase() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Oggi · $todayName")
        s.unfinished?.let { u ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Allenamento in corso: ${u.title}", style = MaterialTheme.typography.titleMedium)
                    Text("Iniziato alle ${formatTime(u.startedAt)}")
                    Button(onClick = { onOpenWorkout(u.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                        Text("Riprendi", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        if (s.program == null) {
            Text("Nessun programma attivo. Creane uno o attivane uno dalla sezione Schede.")
        } else if (s.unfinished == null) {
            val today = s.today
            if (today != null) {
                Text(today.name, style = MaterialTheme.typography.displaySmall)
                Text(s.program!!.name, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { vm.start(today.id, onOpenWorkout) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Text("Inizia", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Text("Oggi riposo", style = MaterialTheme.typography.displaySmall)
                Text("Nessuna scheda assegnata a questo giorno. Puoi comunque sceglierne una qui sotto.")
            }
        }
        if (s.unfinished == null) {
            if (s.otherDays.isNotEmpty()) Text("Altre schede", style = MaterialTheme.typography.titleMedium)
            s.otherDays.forEach { d ->
                ListItem(
                    headlineContent = { Text(d.name) },
                    supportingContent = { Text(weekdayLabel(d.dayOfWeek)) },
                    modifier = Modifier.clickable { vm.start(d.id, onOpenWorkout) },
                )
                HorizontalDivider()
            }
            OutlinedButton(onClick = { vm.start(null, onOpenWorkout) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Allenamento libero")
            }
        }
    }
}
