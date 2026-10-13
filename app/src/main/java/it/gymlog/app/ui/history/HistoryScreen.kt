package it.gymlog.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.SessionSource
import it.gymlog.app.ui.common.ScreenTitle
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate

@Composable
fun HistoryScreen(onOpen: (Long) -> Unit) {
    val vm = appViewModel { HistoryViewModel(it.workouts) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Storico", Modifier.padding(horizontal = 16.dp))
        if (sessions.isEmpty()) {
            Text("Nessun allenamento. Puoi importare lo storico da Hevy nelle Impostazioni.", Modifier.padding(16.dp))
        }
        LazyColumn {
            items(sessions, key = { it.id }) { s ->
                ListItem(
                    headlineContent = { Text(s.title) },
                    supportingContent = { Text("${formatDate(s.startedAt)} · ${s.setCount} serie") },
                    trailingContent = { if (s.source == SessionSource.HEVY_IMPORT) AssistChip(onClick = { onOpen(s.id) }, label = { Text("Hevy") }) },
                    modifier = Modifier.clickable { onOpen(s.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
