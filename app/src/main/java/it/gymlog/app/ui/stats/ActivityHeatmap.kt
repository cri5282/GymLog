package it.gymlog.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.gymlog.app.domain.weekStart
import it.gymlog.app.ui.design.Gym
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CELL = 12.dp
private val GAP = 3.dp

@Composable
private fun levelColor(level: Int): Color {
    val c = Gym.colors
    return when (level) {
        0 -> c.raised
        1 -> c.accent.copy(alpha = 0.3f)
        2 -> c.accent.copy(alpha = 0.5f)
        3 -> c.accent.copy(alpha = 0.75f)
        else -> c.accent
    }
}

/** Mappa di attività in stile GitHub: 53 settimane × 7 giorni, aperta sulla settimana corrente. */
@Composable
fun ActivityHeatmap(levels: Map<LocalDate, Int>, today: LocalDate) {
    val c = Gym.colors
    val lastWeek = weekStart(today)
    val weeks = (52 downTo 0).map { lastWeek.minusWeeks(it.toLong()) }
    val listState = rememberLazyListState()
    val activeDays = levels.count { (day, level) -> !day.isAfter(today) && level > 0 }
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
    LaunchedEffect(Unit) { listState.scrollToItem(weeks.lastIndex) }
    Column(Modifier.semantics { contentDescription = "Mappa di attività: $activeDays giorni con allenamento negli ultimi 12 mesi" }) {
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(GAP)) {
            items(weeks.size) { i ->
                val week = weeks[i]
                val weekActive = (0L until 7L).count { d ->
                    val day = week.plusDays(d)
                    !day.isAfter(today) && (levels[day] ?: 0) > 0
                }
                Column(
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Settimana dal ${week.format(fmt)}: $weekActive giorni con allenamento"
                    },
                    verticalArrangement = Arrangement.spacedBy(GAP),
                ) {
                    val monthStart = (0L until 7L).map { week.plusDays(it) }.firstOrNull { it.dayOfMonth == 1 }
                    Box(Modifier.height(14.dp).width(CELL)) {
                        if (monthStart != null || i == 0) {
                            Text((monthStart ?: week).month.getDisplayName(TextStyle.SHORT, Locale.ITALIAN),
                                style = MaterialTheme.typography.labelSmall, color = c.textSecondary, softWrap = false,
                                modifier = Modifier.width(32.dp))
                        }
                    }
                    (0L until 7L).forEach { d ->
                        val day = week.plusDays(d)
                        val m = Modifier.size(CELL).background(
                            if (day.isAfter(today)) Color.Transparent else levelColor(levels[day] ?: 0), RoundedCornerShape(3.dp),
                        )
                        Box(if (day == today) m.border(1.dp, c.text, RoundedCornerShape(3.dp)) else m)
                    }
                }
            }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Text("Meno", style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.padding(end = 4.dp))
            (0..4).forEach { Box(Modifier.size(CELL).background(levelColor(it), RoundedCornerShape(3.dp))) }
            Text("Più", style = MaterialTheme.typography.labelSmall, color = c.textSecondary, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
