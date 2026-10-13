package it.gymlog.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.gymlog.app.data.db.SessionTime
import it.gymlog.app.domain.DayMarker
import it.gymlog.app.ui.common.appViewModel
import it.gymlog.app.ui.common.formatDate
import it.gymlog.app.ui.design.AccentPill
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymCard
import it.gymlog.app.ui.design.GymType
import it.gymlog.app.ui.design.IconBadge
import it.gymlog.app.ui.design.RoundIconButton
import it.gymlog.app.ui.design.ScreenHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val headerDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)

fun SessionTime.minutes(): Long? = endedAt?.let { (it - startedAt).coerceAtLeast(0) / 60_000 }

@Composable
fun HomeScreen(
    onOpenWorkout: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onPickRoutine: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it.programs, it.workouts) }
    LifecycleResumeEffect(Unit) { vm.refreshDay(); onPauseOrDispose {} }
    val s by vm.state.collectAsStateWithLifecycle()
    val c = Gym.colors
    if (s.loading) return

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("GymLog", s.today.format(headerDate).replaceFirstChar { it.uppercase() }) {
            RoundIconButton(Icons.Rounded.Settings, "Impostazioni", onOpenSettings)
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GymCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::previousWeek) { Icon(Icons.Rounded.ChevronLeft, "Settimana precedente", tint = c.text) }
                    Text(
                        if (s.weekStart == it.gymlog.app.domain.weekStart(s.today)) "Questa settimana"
                        else "Settimana del ${s.weekStart.format(shortDate)}",
                        color = c.textSecondary, modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = vm::nextWeek) { Icon(Icons.Rounded.ChevronRight, "Settimana successiva", tint = c.text) }
                }
                Row(Modifier.padding(vertical = 8.dp)) {
                    s.days.forEachIndexed { i, day -> DayCell(day, s.markers.getOrNull(i), day == s.today, day == s.selected) { vm.select(day) } }
                }
                SelectedDayRow(s, onStart = { id -> vm.start(id, onOpenWorkout) }, onResume = onOpenWorkout,
                    onOpenSession = onOpenSession, onPickRoutine = onPickRoutine)
                if (s.selected == s.today && s.unfinished == null && s.selectedRoutine != null) {
                    androidx.compose.material3.TextButton(onClick = onPickRoutine, modifier = Modifier.fillMaxWidth()) {
                        Text("Fai un'altra scheda o un allenamento libero", color = c.accent)
                    }
                }
            }

            GymCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFF9F1C), modifier = Modifier.size(28.dp))
                    Text(
                        when (s.streak) { 0 -> "Inizia la tua serie"; 1 -> "1 settimana di fila"; else -> "${s.streak} settimane di fila" },
                        style = GymType.cardTitle, color = c.text,
                    )
                }
                Text("${s.thisWeekDone}/${s.plannedPerWeek} questa settimana · ${s.totalWorkouts} allenamenti in totale",
                    color = c.textSecondary, modifier = Modifier.padding(top = 4.dp))
            }

            s.last?.let { last ->
                GymCard(onClick = { onOpenSession(last.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(Icons.Rounded.History, color = c.raised)
                        Column(Modifier.weight(1f)) {
                            Text("ULTIMO ALLENAMENTO", style = GymType.label, color = c.textSecondary)
                            Text(last.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text(buildString {
                                append(formatDate(last.startedAt))
                                last.minutes()?.let { append(" · $it min") }
                                append(" · ${last.setCount} serie")
                            }, color = c.textSecondary)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DayCell(
    day: LocalDate, marker: DayMarker?, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit,
) {
    val c = Gym.colors
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .background(if (isSelected && !isToday) c.raised else Color.Transparent).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALIAN).take(2).uppercase(),
            style = GymType.label, color = c.textSecondary)
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (isToday) c.accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text("${day.dayOfMonth}", fontWeight = FontWeight.Bold,
                color = if (isToday) c.onAccent else c.text, style = MaterialTheme.typography.titleMedium)
        }
        Box(Modifier.size(6.dp).clip(CircleShape).background(
            when (marker) { DayMarker.DONE -> c.accent; DayMarker.PLANNED -> c.textSecondary; else -> Color.Transparent },
        ))
    }
}

@Composable
private fun SelectedDayRow(
    s: HomeState, onStart: (Long) -> Unit, onResume: (Long) -> Unit, onOpenSession: (Long) -> Unit, onPickRoutine: () -> Unit,
) {
    val c = Gym.colors
    val label = if (s.selected == s.today) "OGGI"
    else s.selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALIAN).uppercase()
    val routine = s.selectedRoutine
    val session = s.selectedSession
    Row(
        Modifier.padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(c.raised).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            when { session != null -> Icons.Rounded.CheckCircle; routine != null -> Icons.Rounded.FitnessCenter; else -> Icons.Rounded.SelfImprovement },
            color = if (routine == null && session == null) c.border else c.accent,
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = GymType.label, color = c.textSecondary)
            Text(session?.title ?: routine?.name ?: "Riposo", style = MaterialTheme.typography.titleLarge, color = c.text)
            if (session != null) Text("Fatto${session.minutes()?.let { " · $it min" } ?: ""}", color = c.accent)
            else if (routine != null) Text("${s.selectedExerciseCount} esercizi", color = c.textSecondary)
        }
        val unfinished = s.unfinished
        when {
            unfinished != null && (routine == null || unfinished.routineDayId == routine.id) && s.selected == s.today ->
                AccentPill("Riprendi", color = c.resume, onClick = { onResume(unfinished.id) })
            session != null -> AccentPill("Vedi", onClick = { onOpenSession(session.id) })
            routine != null && !s.selected.isBefore(s.today) && unfinished == null -> AccentPill("Start", onClick = { onStart(routine.id) })
            routine == null && s.selected == s.today && unfinished == null -> AccentPill("Scegli", onClick = onPickRoutine)
        }
    }
}
