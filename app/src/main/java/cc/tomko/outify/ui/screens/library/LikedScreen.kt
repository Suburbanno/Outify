package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Artist
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.SwipeableTrackRow
import cc.tomko.outify.ui.notifications.InAppNotificationController
import cc.tomko.outify.ui.notifications.NotificationSpec
import cc.tomko.outify.ui.viewmodel.library.LikedViewModel
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.LikedScreen(
    viewModel: LikedViewModel,
    listState: LazyListState,
    onBack: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onArtworkClick: (Album) -> Unit,
) {
    val spirc = viewModel.spirc
    val tracks by viewModel.likedTracks.collectAsState()

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
                                    .data(ALBUM_COVER_URL + url)
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

    val currentTrack by viewModel.currentTrack().collectAsState(initial = null)
    val totalCount by viewModel.totalCount.collectAsState()

    var transitioningTrackUri by remember { mutableStateOf<String?>(null) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val collapsingState = rememberCollapsingHeaderState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val canExpand =
                listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0

            collapsingState.snapIfNeeded(canExpand)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                listState.scrollToItem(0)
                viewModel.refresh()
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
            .nestedScroll(collapsingState.nestedScrollConnection)
    ) {
        val currentTopBarHeightDp =
            with(density) { collapsingState.height.value.toDp() }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = currentTopBarHeightDp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = tracks,
                key = { it.uri },
                contentType = { "track" }
            ) { track ->
                SwipeableTrackRow(
                    track,
                    currentTrack = currentTrack,
                    isTransitioning = transitioningTrackUri == track.uri,
                    onRowClick = remember(track.uri) {
                        {
                            transitioningTrackUri = track.uri
                            spirc.load(null, track.uri)
                            // Optimistic UI
                            viewModel.setTrack(track)
                        }
                    },
                    onRowLongClick = {
                    },
                    onArtworkClick = {
                        transitioningTrackUri = track.uri
                        onArtworkClick(track.album!!)
                    },
                    onArtistClick = { artist ->
                        transitioningTrackUri = track.uri
                        onArtistClick(artist)
                    },
                    onAddToQueue = { track ->
                        InAppNotificationController.show(
                            NotificationSpec(
                                message = "Added to queue",
                                icon = {
                                    Icon( Icons.Default.Queue, contentDescription = "Queue")
                                }
                            )
                        )

                        spirc.addToQueue(track.uri)
                    },
                    onStartRadio = { track ->
                        InAppNotificationController.show(
                            NotificationSpec(
                                message = "Radio started",
                                icon = {
                                    Icon( Icons.Default.Radio, contentDescription = "Radio")
                                }
                            )
                        )

                        spirc.startRadio(track.uri, false)
                    }
                )
            }
        }

        CollapsingHeader(
            collapseFraction = collapsingState.collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackPressed = onBack,
            backgroundContent = {
                val artworkUrl by viewModel.getArtwork().collectAsState(initial = null)
                ArtworkBackground(
                    artworkUrl = ALBUM_COVER_URL + artworkUrl,
                )
            },
            titleContent = {
                Text(
                    text = "Your liked tracks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "By you • $totalCount songs",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            fabContent = {
                LargeExtendedFloatingActionButton(
                    onClick = {
                        spirc.shuffleLoad()
                    },
                    shape = MaterialShapes.Cookie9Sided.toShape()
                ) {
                    Icon(Icons.Rounded.Shuffle, null)
                }
            }
        )
    }
}
