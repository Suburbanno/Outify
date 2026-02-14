package cc.tomko.outify.ui.screens.library.artist
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.viewmodel.library.ArtistUiState
import cc.tomko.outify.ui.viewmodel.library.ArtistViewModel
import cc.tomko.outify.utils.SharedElementKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Shout out to PixelPlay.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.ArtistLikedTracksScreen(
    viewModel: ArtistViewModel,
    onArtworkClick: (Track) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    when (uiState) {
        ArtistUiState.Loading -> {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
        }

        is ArtistUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (uiState as ArtistUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is ArtistUiState.Success -> {
            val artist = (uiState as ArtistUiState.Success).artist
            val artworkUrl = ALBUM_COVER_URL + artist.getCover(CoverSize.LARGE)?.uri

            val likedTracks by viewModel.likedTracks.collectAsState()
            val likedTrackCount = likedTracks.size

            val currentTrack = OutifyApplication.playbackStateHolder.state.collectAsState().value.currentTrack

            val lazyList = rememberLazyListState()

            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val minTopBarHeight = 64.dp + statusBarHeight
            val maxTopBarHeight = 300.dp

            val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
            val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

            val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
            var collapseFraction by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(topBarHeight.value) {
                collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
            }

            val nestedScroll = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val delta = available.y
                        val isScrollingDown = delta < 0

                        if(!isScrollingDown && (lazyList.firstVisibleItemIndex > 0 || lazyList.firstVisibleItemScrollOffset > 0)) {
                            return Offset.Zero
                        }

                        val previousHeight = topBarHeight.value
                        val height = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                        val consumed = height - previousHeight

                        if(consumed.roundToInt() != 0){
                            coroutineScope.launch {
                                topBarHeight.snapTo(height)
                            }
                        }

                        val canConsumeScroll = !(isScrollingDown && height == minTopBarHeightPx)
                        return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        return super.onPostFling(consumed, available)
                    }
                }
            }

            LaunchedEffect(lazyList.isScrollInProgress) {
                if(!lazyList.isScrollInProgress) {
                    val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
                    val canExpand = lazyList.firstVisibleItemIndex == 0 && lazyList.firstVisibleItemScrollOffset == 0

                    val targetValue = if (shouldExpand && canExpand) {
                        maxTopBarHeightPx
                    } else {
                        minTopBarHeightPx
                    }

                    if (topBarHeight.value != targetValue) {
                        coroutineScope.launch {
                            topBarHeight.animateTo(
                                targetValue,
                                spring(stiffness = Spring.StiffnessMedium)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface)
                    .nestedScroll(nestedScroll)
            ) {
                val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

                val topPadding = currentTopBarHeightDp + if(likedTrackCount > 0) 8.dp else 0.dp
                LazyColumn(
                    state = lazyList,
                    contentPadding = PaddingValues(
                        top = topPadding,
                        start = 16.dp,
                        end = if ((lazyList.canScrollForward || lazyList.canScrollBackward) && collapseFraction > 0.95f) 24.dp else 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(likedTracks, key = { track -> "artist_song_${track.uri}" }) { track ->
                        val trackArtworkUrl: String = remember(track.album?.uri) {
                            track.album?.getCover(CoverSize.MEDIUM)?.uri.let { ALBUM_COVER_URL + it }
                        }

                        TrackRow(
                            title = track.name,
                            artist = track.artists.joinToString { it.name },
                            artworkUrl = trackArtworkUrl,
                            isPlaying = currentTrack?.uri.equals(track.uri),
                            isSelected = false,
                            onRowClick = remember(track.uri) { { Spirc.load(track.uri) } },
                            onArtworkClick = {onArtworkClick(track)},
                            sharedTransitionKey = null
                        )
                    }
                }

//                if (collapseFraction > 0.95f) {
//                    ExpressiveScrollBar(
//                        listState = lazyListState,
//                        modifier = Modifier
//                            .align(Alignment.CenterEnd)
//                            .padding(
//                                top = currentTopBarHeightDp + 12.dp,
//                                bottom = fabBottomPadding + 80.dp
//                            )
//                    )
//                }

                CollapsingArtistTopBar(
                    artist = artist,
                    likedSongs = likedTrackCount,
                    collapseFraction = collapseFraction,
                    headerHeight = currentTopBarHeightDp,
                    onBackPressed = onBack,
                    artworkUrl = artworkUrl,
                    onPlayClick = {
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SharedTransitionScope.CollapsingArtistTopBar(
    artist: Artist,
    likedSongs: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    artworkUrl: String,
    onBackPressed: () -> Unit,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current

    val surfaceColor = MaterialTheme.colorScheme.surface
    val statusBarColor = Color.Black.copy(alpha = 0.6f)
//        if (MaterialTheme.current) Color.Black.copy(alpha = 0.6f) else Color.White.copy(
//            alpha = 0.4f
//        )

    // Animation Values
    val fabScale = 1f - collapseFraction
    val backgroundAlpha = collapseFraction
    val headerContentAlpha = 1f - (collapseFraction * 2).coerceAtMost(1f)

    // Title animation
    val titleScale = lerp(1f, 0.75f, collapseFraction)
    val titlePaddingStart = lerp(24.dp, 58.dp, collapseFraction)
    val titleMaxLines = if (collapseFraction < 0.5f) 2 else 1
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val animatedTitleAlignment =
        BiasAlignment(horizontalBias = -1f, verticalBias = titleVerticalBias)
    val titleContainerHeight = lerp(88.dp, 56.dp, collapseFraction)
    val yOffsetCorrection = lerp((titleContainerHeight / 2) - 64.dp, 0.dp, collapseFraction)

    val imageRequest = ImageRequest.Builder(context)
        .data(artworkUrl)
        .allowHardware(true)
        .build()
    val imageLoader = (context.applicationContext as OutifyApplication).imageLoader

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(surfaceColor.copy(alpha = backgroundAlpha))
    ) {
        // Header Content (visible when expanded)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = headerContentAlpha }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        rememberSharedContentState(SharedElementKey.ARTIST_DETAILS_TOPBAR_IMAGE),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current
                    )
            ) {
                AsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentDescription = "Artwork",
                    modifier = Modifier.fillMaxSize()
                        .sharedBounds(
                            rememberSharedContentState(SharedElementKey.ALBUM_ARTWORK + "_${artworkUrl}"),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
                        ),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.4f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        // Status bar gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            statusBarColor,
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Top bar content (buttons, title, etc.)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            FilledIconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 4.dp),
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Box(
                modifier = Modifier
                    .align(animatedTitleAlignment)
                    .height(titleContainerHeight)
                    .fillMaxWidth()
                    .offset(y = yOffsetCorrection)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = titlePaddingStart, end = 120.dp)
                        .graphicsLayer {
                            scaleX = titleScale
                            scaleY = titleScale
                        }
                        .sharedBounds(
                            rememberSharedContentState(SharedElementKey.ARTIST_DETAILS_TOPBAR_TEXT),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
                        ),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your liked ${artist.name} songs",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$likedSongs liked songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LargeExtendedFloatingActionButton(
                onClick = onPlayClick,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                        alpha = fabScale
                    }
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle play album")
            }
        }
    }
}