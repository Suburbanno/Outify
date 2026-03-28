package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.core.model.Album
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.OutifyUri
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.core.model.toSpotifyUri
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.AlbumRow
import cc.tomko.outify.ui.components.user.UserChipAvatar
import cc.tomko.outify.ui.viewmodel.library.TrackViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.TrackDetailScreen(
    viewModel: TrackViewModel,
    onBack: () -> Unit,
    artistClick: (uri: String) -> Unit,
    albumClick: (Album) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    when {
        uiState?.error != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState?.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        uiState?.track != null -> {
            val track = uiState?.track!!
            val artworkUrl = ALBUM_COVER_URL + track.album?.getCover(CoverSize.LARGE)?.uri
            val currentTrack by viewModel.currentTrack.collectAsState(initial = null)
            val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
            val spirc = viewModel.spirc
            val isLiked by viewModel.isLiked(track.id).collectAsState(initial = false)

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
                    item {

                    }

                    item {
                        Text(
                            text = if (track.artists.count() > 1) "Artists" else "Artist",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    items(track.artists) { artist ->
                        println(artist)
                        val artwork = artist.getCover(CoverSize.LARGE)?.uri?.let { ALBUM_COVER_URL + it}
                        println(artwork)

                        UserChipAvatar(
                            artworkUrl = artwork,
                            size = 56.dp,
                            modifier = Modifier
                                .clickable {
                                    artistClick(artist.uri)
                                }
                        )
                    }

                    if(track.album != null) {
                        item {
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            AlbumRow(
                                album = track.album,
                                artworkUrl = artworkUrl,
                                onRowClick = { albumClick(track.album) },
                            )
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
                            fallback = {
                                Icon(Icons.Default.MusicNote, contentDescription = null)
                            }
                        )
                    },
                    titleContent = {
                        Text(
                            text = track.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    fabContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleLike(track.id) }
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isLiked) "Unlike" else "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            LargeExtendedFloatingActionButton(
                                onClick = {
                                    spirc.startRadio(track.toSpotifyUri())
                                },
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            ) {
                                Icon(Icons.Rounded.Radio, null)
                            }
                        }
                    }
                )
            }
        }
    }
}
