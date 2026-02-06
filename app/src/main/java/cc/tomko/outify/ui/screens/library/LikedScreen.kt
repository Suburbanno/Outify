package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
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
import androidx.navigation.NavDirections
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.viewmodel.library.LikedViewModel
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.LikedScreen(
    viewModel: LikedViewModel,
    listState: LazyListState,
    backStack: NavBackStack<NavKey>,
    onTrackClick: (Track) -> Unit,
) {
    val tracks by viewModel.likedTracks.collectAsState()
    LaunchedEffect(Unit) { viewModel.ensureLoaded() }

    val itemCount = tracks.size

    val context = LocalContext.current
    val density = LocalDensity.current

    val imageSizePx = remember(density) {
        with(density) { 56.dp.roundToPx() }
    }

    val imageLoader = (context.applicationContext as OutifyApplication).imageLoader

    // Prefetch images for visible range and trigger viewmodel to load more when near end
    LaunchedEffect(listState, tracks) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { first ->
                withContext(Dispatchers.IO) {
                    val end = (first + 8).coerceAtMost(tracks.lastIndex)
                    for (i in first..end) {
                        tracks.getOrNull(i)?.album?.covers?.firstOrNull()?.uri?.let { url ->
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
                viewModel.onVisibleIndex(first + 8)
            }
    }

    val currentTrack by OutifyApplication
        .playbackManager
        .playbackStateHolder
        .currentTrack
        .collectAsState(initial = null)

    LazyColumn(state = listState) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("Liked Songs", style = MaterialTheme.typography.headlineLargeEmphasized, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))

            Text("$itemCount songs", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
            Spacer(Modifier.height(12.dp))
        }

        items(
            items = tracks,
            key = { it.uri },
            contentType = { "track" }
        ) { track ->
            TrackRow(
                title = track.name,
                artist = track.artists.joinToString { it.name },
                artworkUrl = (OutifyApplication.ALBUM_COVER_URL + track.album?.covers?.first()?.uri),
                isPlaying = currentTrack?.uri.equals(track.uri),
                isSelected = false,
//                trailingContent = TODO(),
                onRowClick = remember(track.uri) { { onTrackClick(track) } },
//                onRowLongClick = TODO(),
                onArtworkClick = {
                    backStack.add(Route.AlbumScreenFromTrack(track))
                },
//                onTitleClick = TODO(),
                onArtistClick = {
                    println("Artistt!")
                },
            )
        }
    }
}
