package it.gymlog.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.gymlog.app.ui.design.Gym

data class NavItem(val route: String, val label: String, val icon: ImageVector)

val leftItems = listOf(NavItem(Routes.HOME, "Home", Icons.Rounded.Home), NavItem(Routes.PLAN, "Piano", Icons.Rounded.CalendarMonth))
val rightItems = listOf(
    NavItem(Routes.STATS, "Statistiche", Icons.Rounded.BarChart),
    NavItem(Routes.EXERCISES, "Esercizi", Icons.AutoMirrored.Rounded.FormatListBulleted),
)

/** Barra inferiore: due voci a sinistra, Start centrale, due voci a destra. */
@Composable
fun BottomBar(current: String?, resuming: Boolean, onNavigate: (String) -> Unit, onStart: () -> Unit) {
    val c = Gym.colors
    val showLabels = LocalDensity.current.fontScale <= 1.3f
    Column(Modifier.fillMaxWidth().background(c.background)) {
        HorizontalDivider(color = c.border.copy(alpha = 0.5f))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            leftItems.forEach { NavCell(it, current == it.route, showLabels, onNavigate) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val color = if (resuming) c.resume else c.accent
                val label = if (resuming) "Riprendi" else "Start"
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-14).dp)) {
                    Box(
                        Modifier.size(64.dp).shadow(10.dp, CircleShape, ambientColor = color, spotColor = color)
                            .background(color, CircleShape).border(4.dp, c.background, CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "$label allenamento", onClick = onStart),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (resuming) Icons.Rounded.PlayArrow else Icons.Rounded.FitnessCenter,
                            if (showLabels) null else "$label allenamento",
                            tint = c.onAccent, modifier = Modifier.size(30.dp),
                        )
                    }
                    if (showLabels) {
                        Text(label, color = color, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            rightItems.forEach { NavCell(it, current == it.route, showLabels, onNavigate) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavCell(
    item: NavItem, selected: Boolean, showLabel: Boolean, onNavigate: (String) -> Unit,
) {
    val c = Gym.colors
    val color = when {
        !selected -> c.textSecondary
        c.dark -> c.accent
        else -> c.text
    }
    Column(
        Modifier.weight(1f).height(64.dp)
            .selectable(selected = selected, role = Role.Tab) { onNavigate(item.route) }
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(item.icon, if (showLabel) null else item.label, tint = color)
        if (showLabel) {
            Text(item.label, color = color, style = MaterialTheme.typography.labelSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
