package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import it.gymlog.app.data.repo.SessionRecap
import it.gymlog.app.ui.common.formatKg

@Composable
fun RecapDialog(recap: SessionRecap, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Allenamento completato") },
        text = {
            Column {
                Text("Durata: ${recap.durationMin} min")
                Text("Serie allenanti: ${recap.workingSets}")
                Text("Volume: ${formatKg(recap.volumeKg)} kg")
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Chiudi") } },
    )
}
