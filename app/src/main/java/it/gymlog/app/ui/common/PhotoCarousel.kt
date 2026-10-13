package it.gymlog.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import java.io.File

/** Foto del macchinario a scorrimento; tap = schermo intero; file mancante = segnaposto. */
@Composable
fun PhotoCarousel(paths: List<String>, modifier: Modifier = Modifier, height: Int = 180) {
    if (paths.isEmpty()) return
    val pager = rememberPagerState { paths.size }
    var fullscreen by remember { mutableStateOf<String?>(null) }
    Box(modifier.fillMaxWidth().height(height.dp)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            PhotoImage(
                paths[page],
                Modifier.fillMaxSize().padding(horizontal = 4.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { fullscreen = paths[page] },
                ContentScale.Crop,
            )
        }
        if (paths.size > 1) {
            Text(
                "${pager.currentPage + 1}/${paths.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
    fullscreen?.let { path ->
        Dialog(onDismissRequest = { fullscreen = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { fullscreen = null }) {
                PhotoImage(path, Modifier.fillMaxSize(), ContentScale.Fit)
            }
        }
    }
}

@Composable
fun PhotoImage(path: String, modifier: Modifier, scale: ContentScale) {
    SubcomposeAsyncImage(
        model = File(path),
        contentDescription = "Foto del macchinario",
        contentScale = scale,
        modifier = modifier,
        error = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BrokenImage, contentDescription = "Foto non trovata", Modifier.size(40.dp))
            }
        },
    )
}
