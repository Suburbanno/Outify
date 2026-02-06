package cc.tomko.outify.ui.screens.library.album

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.viewmodel.library.album.AlbumViewModel
import cc.tomko.outify.utils.SharedElementKey
import cc.tomko.outify.utils.getRandomMaterialShape
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.AlbumScreen(
    viewModel: AlbumViewModel,
    modifier: Modifier = Modifier,
    sourceTrack: Track? = null, // From what track do we come from
    listState: LazyListState = rememberLazyListState(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel, sourceTrack) {
        viewModel.loadAlbum()
    }

    val context = LocalContext.current

    val imageSize = 72.dp
    val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx() }

    val artworkUrl = OutifyApplication.ALBUM_COVER_URL + (sourceTrack?.album?.covers?.first()?.uri ?: uiState.album?.artworkUrl)
    val imageLoader = remember { (context.applicationContext as OutifyApplication).imageLoader }
    val imageRequest = remember(artworkUrl, imageSizePx) {
        ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(imageSizePx)
            .allowHardware(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val currentTrack by OutifyApplication.playbackManager.playbackStateHolder.currentTrack.collectAsState()

    val albumUri = sourceTrack?.album?.uri ?: uiState.album?.albumUri
    val albumName = sourceTrack?.album?.name ?: uiState.album?.title
    val artists = sourceTrack?.artists?.joinToString { it.name } ?: uiState.album?.artists

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(start = 24.dp, top = 8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .clip(getRandomMaterialShape())
                        .size(250.dp)
                ) {
                    AsyncImage(
                        model = imageRequest,
                        imageLoader = imageLoader,
                        contentDescription = "Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .sharedBounds(
                                rememberSharedContentState(SharedElementKey.ALBUM_ARTWORK + "_${artworkUrl}"),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                    )
                }

                // Album name + play icon
                Column(
                ) {
                    Text(
                        text = albumName ?: "Loading",
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        modifier = Modifier.padding(horizontal = 16.dp))

                    IconButton(
                        onClick = {
                            OutifyApplication.spirc.load(albumUri)
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .padding(top = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play album",
                            modifier = Modifier
                                .size(40.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = artists ?: "Loading",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "Tracks:",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if(uiState.tracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 50.dp, bottom = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.size(100.dp))
                }
            }
        } else {
            items(
                items = uiState.tracks,
                key = { it.uri },
                contentType = { "track" }
            ) { track ->
                TrackRow(
                    title = track.name,
                    artist = track.artists.joinToString { it.name },
                    artworkUrl = artworkUrl,
                    isPlaying = currentTrack?.uri.equals(track.uri),
                    isSelected = false,
                    onRowClick = remember(track.uri) { { OutifyApplication.spirc.load(track.uri) } },
                    onArtistClick = {
                        println("Artistt!")
                    },
                    sharedTransitionKey = null
                )
            }
        }
    }
}