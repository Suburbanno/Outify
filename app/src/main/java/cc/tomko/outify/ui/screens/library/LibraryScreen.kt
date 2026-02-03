package cc.tomko.outify.ui.screens.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.viewmodel.LibraryViewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onTrackClick: (Track) -> Unit,
) {
    val tracks by viewModel.likedTracks.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val imageSizePx = remember(density) {
        with(density) { 56.dp.roundToPx() }
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .crossfade(false)
            .build()
    }

    // Prefetch images for visible range and trigger viewmodel to load more when near end
    LaunchedEffect(listState, tracks) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            // filter out spurious re-emissions
            .collect { first ->
                val end = (first + 8).coerceAtMost((tracks.size).coerceAtLeast(0))
                // enqueue a few images ahead of the viewport
                for (i in first..end) {
                    if (i in tracks.indices) {
                        val url = tracks[i].album?.covers?.getOrNull(0)?.uri
                        if (!url.isNullOrEmpty()) {
                            imageLoader.enqueue(
                                ImageRequest.Builder(context)
                                    .data("https://i.scdn.co/image/" + url)
                                    .size(imageSizePx)
                                    .allowHardware(true)
                                    .build()
                            )
                        }
                    }
                }
                viewModel.onVisibleIndex(end)
            }
    }

    LazyColumn(state = listState) {
        items(
            items = tracks,
            key = { it.uri },
            contentType = { "track" }
        ) { track ->
            val onClick = remember(track.uri) { { onTrackClick(track) } }

            LibraryRow(
                track = track,
                imageLoader = imageLoader,
                imageSizePx = imageSizePx,
                onClick = onClick
            )
        }
    }
}
