package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.Profile
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.SwipeableTrackRow
import cc.tomko.outify.ui.components.user.UserChipAvatar
import cc.tomko.outify.ui.viewmodel.library.PlaylistUiState
import cc.tomko.outify.ui.viewmodel.library.PlaylistViewModel
import coil3.compose.AsyncImage
import com.google.common.collect.Multimaps.index
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.PlaylistScreen(
    viewModel: PlaylistViewModel,
    onArtworkClick: (track: Track) -> Unit,
    onArtistClick: (artist: Artist) -> Unit,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is PlaylistUiState.Loading -> {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        }

        is PlaylistUiState.Error -> {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (uiState as PlaylistUiState.Error).error,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is PlaylistUiState.Success -> {
            val playlist = (uiState as PlaylistUiState.Success).playlist!!
            val tracks = playlist.contents

            val lazyList = rememberLazyListState()
            val currentTrack by viewModel.currentTrack().collectAsState(initial = null)
            val spirc = viewModel.spirc

            var artworkUrl by remember { mutableStateOf("") }
            val authorMap by viewModel.authors.collectAsState()
            var authors by remember { mutableStateOf(emptyList<Profile>()) }
            LaunchedEffect(playlist.uri) {
                artworkUrl = viewModel.getArtworkUrl(playlist)
                authors = viewModel.getAuthors(playlist)
            }
            val showAvatarCount = 4

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
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(tracks, key = { idx,track -> "${track.id}_${idx}" }) { idx,playlistItem ->
                        val track by remember(playlistItem.uri) {
                            viewModel.trackFlow(playlistItem.uri)
                        }.collectAsState(initial = null)

                        if (track != null) {
                            SwipeableTrackRow(
                                track = track!!,
                                currentTrack = currentTrack,
                                onRowClick = remember(playlistItem.uri) { {
                                    spirc.load(playlist.uri, playlistItem.uri)
                                    viewModel.setTrack(track!!)
                                } },
                                onArtworkClick = {onArtworkClick(track!!)},
                                onArtistClick =  { artist ->
                                    onArtistClick(artist)
                                },
                                trailingContent = {
                                    val author by produceState<Profile?>(initialValue = null, key1 = playlistItem.attributes.addedBy) {
                                        value = authorMap[playlistItem.attributes.addedBy ]
                                    }

                                    author?.let {
                                        UserChipAvatar(it)
                                    }
                                }
                            )
                        } else  {
                            LaunchedEffect(playlistItem.uri) {
                                viewModel.getOrLoadTrack(playlistItem.uri)
                            }
                        }
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
                            text = playlist.attributes.name,
                            style = MaterialTheme.typography.headlineMedium,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            Box {
                                authors.take(showAvatarCount).forEachIndexed { index, user ->
                                    UserChipAvatar(
                                        profile = user,
                                        size = 20.dp,
                                        modifier = Modifier
                                            .offset(x = (index * 12).dp)
                                            .zIndex((showAvatarCount - index).toFloat())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width((authors.take(showAvatarCount).size * 12).dp))

                            Text(
                                text = "• ${tracks.size} songs",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    fabContent = {
                        LargeExtendedFloatingActionButton(
                            onClick = {
                                spirc.shuffleLoad(playlist.uri)
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