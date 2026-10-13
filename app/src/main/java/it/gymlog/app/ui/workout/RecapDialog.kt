package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.repo.SessionRecap
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.PrimaryButton
import it.gymlog.app.ui.design.StatTile

/** Riepilogo di fine allenamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapSheet(recap: SessionRecap, records: List<String>, onClose: () -> Unit) {
    val c = Gym.colors
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.background) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Allenamento completato 💪", style = GymType.cardTitle, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Rounded.Timer, "Durata", "${recap.durationMin} min", Modifier.weight(1f))
                StatTile(Icons.Rounded.Repeat, "Serie", "${recap.workingSets}", Modifier.weight(1f))
            }
            StatTile(Icons.Rounded.FitnessCenter, "Volume", "${formatKg(recap.volumeKg)} kg")
            if (records.isNotEmpty()) GymCard(color = c.accentContainer) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = c.accent)
                    Text("Nuovi record", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = c.text)
                }
                records.forEach { Text("• $it", color = c.text, modifier = Modifier.padding(top = 4.dp)) }
            }
            PrimaryButton("Chiudi", onClose, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}
