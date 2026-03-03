package cc.tomko.outify.ui.screens.library.album

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.getCover
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.SwipeableTrackRow
import cc.tomko.outify.ui.notifications.InAppNotificationController
import cc.tomko.outify.ui.notifications.NotificationSpec
import cc.tomko.outify.ui.viewmodel.library.album.AlbumViewModel

/**
 * Shout out to PixelPlay.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.AlbumDetailScreen(
    viewModel: AlbumViewModel,
    onBack: () -> Unit,
    artistClick: (uri: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
        }

        uiState.error != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        uiState.album != null -> {
            val album = uiState.album!!
            val tracks = uiState.tracks
            val artworkUrl = ALBUM_COVER_URL + album.getCover(CoverSize.LARGE)?.uri
            val currentTrack by viewModel.currentTrack().collectAsState(initial = null)
            val spirc = viewModel.spirc

            val lazyList = rememberLazyListState()

            val collapsingState = rememberCollapsingHeaderState()

            LaunchedEffect(lazyList.isScrollInProgress) {
                if (!lazyList.isScrollInProgress) {
                    val canExpand =
                        lazyList.firstVisibleItemIndex == 0 &&
                                lazyList.firstVisibleItemScrollOffset == 0

                    collapsingState.snapIfNeeded(canExpand)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface)
                    .nestedScroll(collapsingState.nestedScrollConnection)
            ) {
                val currentTopBarHeightDp =
                    with(density) { collapsingState.height.value.toDp() }

                LazyColumn(
                    state = lazyList,
                    contentPadding = PaddingValues(
                        top = currentTopBarHeightDp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(tracks, key = { track -> "album_song_${track.uri}" }) { track ->
                        SwipeableTrackRow(
                            track = track,
                            currentTrack = currentTrack,
                            onRowClick = remember(track.uri) { {
                                viewModel.setTrack(track)
                                spirc.load(album.uri,track.uri)
                            } },
                            onArtistClick = { artistClick(it.uri) },
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
                        ArtworkBackground(
                            artworkUrl = artworkUrl,
                        )
                    },
                    titleContent = {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${album.artists.joinToString { it.name }} • ${tracks.size} songs",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    fabContent = {
                        LargeExtendedFloatingActionButton(
                            onClick = {
                                spirc.shuffleLoad(album.uri)
                            },
                            shape = MaterialShapes.Cookie9Sided.toShape()
                        ) {
                            Icon(Icons.Rounded.Shuffle, null)
                        }
                    }
                )
            }
        }
    }
}