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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.domain.localDate
import it.gymlog.app.ui.common.ConfirmDialog
import it.gymlog.app.ui.common.LocalSnackbar
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.design.EmptyState
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.ScreenHeader
import it.gymlog.app.ui.design.SectionLabel
import it.gymlog.app.ui.stats.SessionCard
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val vm = appViewModel { HistoryViewModel(it.workouts) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    var deleting by remember { mutableStateOf<SessionTime?>(null) }
    val zone = ZoneId.systemDefault()
    val byMonth = sessions.groupBy { localDate(it.startedAt, zone).withDayOfMonth(1) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro", tint = Gym.colors.text) }
            }
            ScreenHeader("Storico", "${sessions.size} allenamenti")
            if (sessions.isEmpty()) EmptyState(Icons.Rounded.History, "Nessun allenamento. Puoi importare lo storico da Hevy nelle Impostazioni.")
        }
        byMonth.forEach { (month, list) ->
            item(key = "m$month") { SectionLabel(month.format(monthFormat), Modifier.padding(start = 16.dp, top = 8.dp)) }
            items(list, key = { it.id }) { s ->
                Row(Modifier.padding(horizontal = 16.dp)) {
                    SessionCard(s, onClick = { onOpen(s.id) }, onDelete = { deleting = s })
                }
            }
        }
    }
    deleting?.let { session ->
        ConfirmDialog(
            "Eliminare l'allenamento?",
            "\"${session.title}\" e le sue ${session.setCount} serie verranno eliminate dallo storico.",
            "Elimina",
            onConfirm = {
                deleting = null
                vm.deleteSession(session.id) { deleted ->
                    if (deleted != null) {
                        snackbar?.showUndo("Allenamento eliminato") { vm.restoreSession(deleted) }
                    }
                }
            },
            onDismiss = { deleting = null },
        )
    }
}
