package cc.tomko.outify.ui.screens.library

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.viewmodel.LikedViewModel
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LikedScreen(
    viewModel: LikedViewModel,
    listState: LazyListState,
    onTrackClick: (Track) -> Unit,
) {
    val tracks by viewModel.likedTracks.collectAsState()
    LaunchedEffect(viewModel) { viewModel.ensureLoaded() }

    val itemCount = tracks.size

    val context = LocalContext.current
    val density = LocalDensity.current

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
                                    .data(OutifyApplication.ALBUM_COVER_URL + url)
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
        item {
            Spacer(Modifier.height(16.dp))
            Text("Liked Songs", style = MaterialTheme.typography.headlineLargeEmphasized, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

            Text("$itemCount songs", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
        }

        items(
            items = tracks,
            key = { it.uri },
            contentType = { "track" }
        ) { track ->
            val onClick = remember(track.uri) { { onTrackClick(track) } }
            val currentTrack by OutifyApplication.playbackManager.playbackStateHolder.currentTrack.collectAsState()

            TrackRow(
                title = track.name,
                artist = track.artists.joinToString { it.name },
                artworkUrl = (OutifyApplication.ALBUM_COVER_URL + track.album?.covers?.first()?.uri),
                isPlaying = currentTrack?.uri.equals(track.uri),
                isSelected = false,
//                trailingContent = TODO(),
                onRowClick = onClick,
//                onRowLongClick = TODO(),
                onArtworkClick = {
                    println("Album!")
                },
//                onTitleClick = TODO(),
                onArtistClick = {
                    println("Artistt!")
                },
            )
        }
    }
}
