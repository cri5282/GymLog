package it.gymlog.app.ui.workout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.gymlog.app.ui.design.GymChip

/** Barra orizzontale per saltare rapidamente da un esercizio all'altro. */
@Composable
fun ExerciseChips(pages: List<PageState>, current: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        if (current in pages.indices) listState.animateScrollToItem(current)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        itemsIndexed(pages, key = { i, p -> "${p.planned.order}:$i" }) { index, page ->
            val exercise = page.planned.exercise.exercise
            val done = page.rows.count { it.done }
            val status = when {
                page.rows.isEmpty() -> "nessuna serie"
                done == 0 -> "da iniziare"
                done == page.rows.size -> "completato"
                else -> "in corso"
            }
            GymChip(
                text = "${index + 1} · ${exercise.name}",
                selected = index == current,
                onClick = { onSelect(index) },
                maxLines = 1,
                selectableRole = Role.Tab,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .widthIn(max = 220.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Esercizio ${index + 1}: ${exercise.name}, $done di ${page.rows.size} serie, $status"
                    },
            )
        }
    }
}
