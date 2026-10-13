package it.gymlog.app.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.db.RoutineDay
import it.gymlog.app.ui.common.weekdayLabel
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.IconBadge

/** Pannello "Scegli una scheda": schede del programma attivo + allenamento libero (null). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinePickerSheet(days: List<RoutineDay>, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Gym.colors.background) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Scegli una scheda", style = GymType.cardTitle, color = Gym.colors.text)
            days.forEach { d -> PickRow(d.name, weekdayLabel(d.dayOfWeek), Icons.Rounded.FitnessCenter) { onPick(d.id) } }
            PickRow("Allenamento libero", "Aggiungi gli esercizi mentre ti alleni", Icons.Rounded.Bolt) { onPick(null) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PickRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GymCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon)
            Column(Modifier.weight(1f)) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Gym.colors.text)
                Text(subtitle, color = Gym.colors.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Gym.colors.textSecondary)
        }
    }
}
