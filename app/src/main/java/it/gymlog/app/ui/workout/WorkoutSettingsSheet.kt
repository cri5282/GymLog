package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.settings.Settings
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymSwitchRow
import it.gymlog.app.ui.design.SectionLabel

/** Impostazioni rapide modificabili senza uscire dall'allenamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSettingsSheet(
    settings: Settings,
    onDismiss: () -> Unit,
    onTimerSound: (Boolean) -> Unit,
    onTimerVibration: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onShowExerciseChips: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
) {
    val c = Gym.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.card,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text(
                "Impostazioni allenamento",
                style = MaterialTheme.typography.titleLarge,
                color = c.text,
            )
            Text(
                "Le modifiche valgono subito e restano salvate.",
                color = c.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            SectionLabel("Timer")
            GymSwitchRow("Vibrazione a fine recupero", settings.timerVibration, onTimerVibration)
            HorizontalDivider(color = c.border.copy(alpha = 0.5f))
            GymSwitchRow("Suono a fine recupero", settings.timerSound, onTimerSound)
            SectionLabel("Allenamento")
            GymSwitchRow("Schermo sempre acceso", settings.keepScreenOn, onKeepScreenOn)
            HorizontalDivider(color = c.border.copy(alpha = 0.5f))
            GymSwitchRow(
                "Chip degli esercizi",
                settings.showExerciseChips,
                onShowExerciseChips,
                supportingText = "Barra rapida sopra la tabella delle serie.",
            )
            HorizontalDivider(color = c.border.copy(alpha = 0.5f))
            GymSwitchRow(
                "Feedback aptico",
                settings.haptics,
                onHaptics,
                supportingText = "Vibrazione su conferma e rimozione di una serie.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
