package it.gymlog.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.design.Gym
import it.gymlog.app.ui.design.GymType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Volume settimanale: una barra per settimana, l'ultima (corrente) in risalto. */
@Composable
fun VolumeBarChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val c = Gym.colors
    val max = points.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val fmt = DateTimeFormatter.ofPattern("d/M", Locale.ITALIAN)
    Row(modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        points.forEachIndexed { index, (week, volume) ->
            Column(
                Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) {
                    contentDescription = "Settimana del ${week.format(fmt)}: " +
                        if (volume > 0) "${formatKg(volume)} kg di volume" else "nessun dato"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (volume > 0) compactKg(volume) else "", style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary, maxLines = 1)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (volume > 0) {
                        Box(
                            Modifier.fillMaxWidth(0.62f)
                                .fillMaxHeight((volume / max).toFloat().coerceIn(0.03f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (index == points.lastIndex) c.accent else c.accent.copy(alpha = 0.55f))
                        )
                    }
                }
                Text(week.format(fmt), style = MaterialTheme.typography.labelSmall, color = c.textSecondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Serie settimanali per muscolo: barra proporzionale al gruppo più allenato. */
@Composable
fun MuscleSetBars(items: List<Pair<Muscle, Int>>) {
    val c = Gym.colors
    val max = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (muscle, count) ->
            Row(
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${muscle.label}: $count serie"
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(muscle.label, color = c.textSecondary, modifier = Modifier.width(132.dp),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(c.raised)) {
                    Box(Modifier.fillMaxWidth(count.toFloat() / max).fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)).background(c.accent))
                }
                Text("$count", style = GymType.number, color = c.text, modifier = Modifier.width(28.dp))
            }
        }
    }
}

/** Linea compatta di un andamento (senza assi): valori in ordine cronologico. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Gym.colors.accent,
    description: String? = null,
) {
    val m = if (description != null) {
        modifier.semantics { contentDescription = description; role = Role.Image }
    } else modifier
    Canvas(m) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0.0001 } ?: 1.0
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / span).toFloat() * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        val lastY = size.height - ((values.last() - min) / span).toFloat() * size.height
        drawCircle(color, radius = 4.dp.toPx(), center = Offset(size.width, lastY))
    }
}

private fun compactKg(v: Double): String =
    if (v >= 1000) "${formatKg(v / 1000)}k" else formatKg(v)
