package it.gymlog.app.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

val CardShape = RoundedCornerShape(20.dp)

/** Scheda arrotondata su sfondo "card". */
@Composable
fun GymCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Gym.colors.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth().clip(CardShape).background(color)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(16.dp)
    Column(m, content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = GymType.label, color = Gym.colors.textSecondary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp))
}

/** Titolo grande di schermata con sottotitolo e azione rotonda a destra. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = GymType.screenTitle, color = Gym.colors.text)
            if (subtitle != null) Text(subtitle, color = Gym.colors.textSecondary)
        }
        action?.invoke()
    }
}

/** Pulsante icona rotondo su sfondo "raised" (es. ingranaggio, orologio). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Gym.colors.raised).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Gym.colors.text) }
}

@Composable
fun GymChip(
    text: String, selected: Boolean = false, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
) {
    val c = Gym.colors
    Box(
        modifier.heightIn(min = 36.dp).clip(CircleShape)
            .background(if (selected) c.accent else c.raised)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) c.onAccent else c.text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            maxLines = 1, softWrap = false)
    }
}

/** Pillola d'accento (testo d'accento su contenitore tenue), es. "Start", suggerimenti. */
@Composable
fun AccentPill(text: String, icon: ImageVector? = null, onClick: (() -> Unit)? = null, color: Color = Gym.colors.accent, modifier: Modifier = Modifier) {
    Row(
        modifier.heightIn(min = 36.dp).clip(CircleShape).background(color.copy(alpha = if (Gym.colors.dark) 0.18f else 0.14f))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, color = color, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
    }
}

/** Valore numerico con − e + ai lati; tap sul valore per scriverlo. */
@Composable
fun Stepper(
    value: String, label: String, onMinus: () -> Unit, onPlus: () -> Unit, onTapValue: () -> Unit,
    modifier: Modifier = Modifier, buttonSize: Int = 44,
) {
    val c = Gym.colors
    Row(
        modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(c.raised),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(buttonSize.dp).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Remove, "Meno $label", tint = c.text)
        }
        Text(value, style = GymType.number, color = c.text, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
            modifier = Modifier.weight(1f).clickable(onClick = onTapValue).padding(vertical = 10.dp))
        Box(Modifier.size(buttonSize.dp).clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, "Più $label", tint = c.text)
        }
    }
}

@Composable
fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    GymCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Gym.colors.textSecondary, modifier = Modifier.size(16.dp))
            Text(label, color = Gym.colors.textSecondary)
        }
        Text(value, style = GymType.stat, color = valueColor ?: Gym.colors.text, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Cerchio di conferma della serie (48 dp): pieno d'accento se fatta, solo bordo altrimenti. */
@Composable
fun CircleCheck(done: Boolean, onClick: () -> Unit, description: String = if (done) "Serie salvata" else "Serie fatta") {
    val c = Gym.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(if (done) c.accent else Color.Transparent)
            .border(BorderStroke(2.dp, if (done) c.accent else c.border), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Check, description, tint = if (done) c.onAccent else c.textSecondary) }
}

/** Pulsante principale a pillola, alto almeno 64 dp. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
                  color: Color? = null, enabled: Boolean = true) {
    val c = Gym.colors
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color ?: c.accent, contentColor = c.onAccent),
    ) {
        if (icon != null) Icon(icon, null, Modifier.padding(end = 8.dp))
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    }
}

/** Pulsante secondario a pillola su "raised". */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Gym.colors
    Surface(onClick = onClick, shape = CircleShape, color = c.raised, contentColor = c.text,
        modifier = modifier.heightIn(min = 56.dp)) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            if (icon != null) Icon(icon, null, Modifier.padding(end = 8.dp))
            Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Gym.colors.textSecondary, modifier = Modifier.size(48.dp))
        Text(text, color = Gym.colors.textSecondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** Quadrato arrotondato colorato con icona (icona di una scheda/routine). */
@Composable
fun IconBadge(icon: ImageVector, color: Color = Gym.colors.accent, size: Int = 40) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Gym.colors.onAccent, modifier = Modifier.size((size * 0.55).dp))
    }
}
