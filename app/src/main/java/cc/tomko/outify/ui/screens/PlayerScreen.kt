package cc.tomko.outify.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.SyncedLyric
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.SmartImage
import cc.tomko.outify.ui.components.WavyMusicSlider
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    listState: LazyListState,
    onArtistClick: (Artist) -> Unit,
    onMoreOptions: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val artworkUrl = uiState.albumArt?.let { ALBUM_COVER_URL + it }
    val positionMs by viewModel.positionMs.collectAsState()
    val currentTrack by viewModel.currentTrack().collectAsState(initial = null)

    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyric by viewModel.currentLyric.collectAsState()

    val isShuffling by viewModel.isShuffling.collectAsState()
    val isRepeating by viewModel.isRepeating.collectAsState()
    val isFavorite by viewModel.isLiked.collectAsState()

    val imageSize = 400.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val screenHeight = maxHeight
        val artworkTopSpacer = ((screenHeight - imageSize) / 3f - 48.dp)
            .coerceAtLeast(16.dp)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .height(screenHeight - 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 24.dp,
                bottom = 96.dp
            )
        ) {
            item { Spacer(modifier = Modifier.height(artworkTopSpacer)) }

            // Album artwork
            item {
                SmartImage(
                    url = artworkUrl,
                    imageSize = imageSize,
                    monochrome = LocalUiSettings.current.monochromePlayer
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isExplicit) {
                            Icon(
                                imageVector = Icons.Filled.Explicit,
                                contentDescription = "Explicit",
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Text(
                            text = uiState.title,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        uiState.artists.forEachIndexed { index, artist ->
                            Text(
                                text = artist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onArtistClick(artist) },
                                    onLongClick = {}
                                )
                            )
                            if (index < uiState.artists.lastIndex) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Time seeker
            item {
                TrackProgressBar(
                    durationMs = uiState.totalLengthMs,
                    positionMs = positionMs,
                    isPlaying = uiState.isPlaying,
                    onSeek = { position ->
                        viewModel.onAction(PlayerAction.SeekTo(position))
                    }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            item {
                PlaybackControls(
                    isPlaying = uiState.isPlaying,
                    isBuffering = uiState.isBuffering,
                    isShuffling = isShuffling,
                    isRepeating = isRepeating,
                    onPlayPause = { viewModel.onAction(PlayerAction.PlayPause) },
                    onNextTrack = { viewModel.onAction(PlayerAction.Next) },
                    onPreviousTrack = { viewModel.onAction(PlayerAction.Previous) },
                    onShuffleChange = { viewModel.onAction(PlayerAction.ShuffleToggle) },
                    onRepeatMode = { viewModel.onAction(PlayerAction.RepeatToggle) },
                )
            }

            item {
                Surface(
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .padding(top = 92.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Lyrics(
                            loadLyrics = { viewModel.loadLyrics() },
                            track = currentTrack,
                            lyrics = lyrics,
                            currentLyric = currentLyric,
                            seekTo = { viewModel.onAction(PlayerAction.SeekTo(it)) },
                            modifier = Modifier.fillMaxSize()
                        )

                        // top gradient fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            Color.Transparent
                                        )
                                    )
                                )
                                .align(Alignment.TopCenter)
                        )

                        // bottom gradient fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                )
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        BottomActionsBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isFavorite = isFavorite,
            onFavoriteToggle = {
                // TODO: Favorite
            },
            onMoreOptions = onMoreOptions
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomActionsBar(
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Favorite toggle
        FilledTonalIconToggleButton(
            checked = isFavorite,
            onCheckedChange = { onFavoriteToggle() },
            modifier = Modifier.size(52.dp),
            shape = MaterialShapes.Cookie6Sided.toShape(),
        ) {
            AnimatedContent(
                targetState = isFavorite,
                transitionSpec = {
                    (scaleIn(
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "favoriteIcon"
            ) { fav ->
                Icon(
                    imageVector = if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (fav) "Remove from favorites" else "Add to favorites"
                )
            }
        }

        IconButton(
            onClick = onMoreOptions,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun TrackProgressBar(
    durationMs: Long,
    positionMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(positionMs, durationMs, isDragging) {
        if (!isDragging && durationMs > 0) {
            sliderValue = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    val displayedPosition = (sliderValue * durationMs).toLong().coerceIn(0L, durationMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "${formatTime(displayedPosition)} / ${formatTime(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )

        WavyMusicSlider(
            value = sliderValue,
            onValueChange = { newValue ->
                isDragging = true
                sliderValue = newValue.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                val newPositionMs = (sliderValue * durationMs).toLong().coerceIn(0L, durationMs)
                onSeek(newPositionMs)
                isDragging = false
            },
            isPlaying = isPlaying
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private enum class PlaybackIconState { Buffering, Playing, Paused }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    isShuffling: Boolean,
    isRepeating: Boolean,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onShuffleChange: () -> Unit,
    onRepeatMode: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconToggleButton(
            checked = isShuffling,
            onCheckedChange = { onShuffleChange() },
            modifier = Modifier.size(42.dp),
            shape = MaterialShapes.Cookie6Sided.toShape(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = "Shuffle mode"
            )
        }

        // Previous
        IconButton(
            onClick = onPreviousTrack,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track")
        }

        // Play / Pause — Cookie9Sided with spring rotation bounce + AnimatedContent
//        var rotated by remember { mutableStateOf(false) }
//        val rotation by animateFloatAsState(
//            targetValue = if (rotated) 20f else 0f,
//            animationSpec = spring(
//                dampingRatio = Spring.DampingRatioMediumBouncy,
//                stiffness = Spring.StiffnessMedium
//            ),
//            label = "playButtonRotation"
//        )

//        LaunchedEffect(rotated) {
//            if (rotated) {
//                delay(300)
//                rotated = false
//            }
//        }

        val iconState = when {
            isBuffering -> PlaybackIconState.Buffering
            isPlaying   -> PlaybackIconState.Playing
            else        -> PlaybackIconState.Paused
        }

        FilledIconButton(
            onClick = {
                if (!isBuffering) {
//                    rotated = true
                    onPlayPause()
                }
            },
            shape = MaterialShapes.Cookie9Sided.toShape(),
            modifier = Modifier
                .size(96.dp),
//                .graphicsLayer { rotationZ = rotation },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            AnimatedContent(
                targetState = iconState,
                transitionSpec = {
                    (scaleIn(
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "playPauseBufferIcon"
            ) { state ->
                when (state) {
                    PlaybackIconState.Buffering -> LoadingIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    PlaybackIconState.Playing -> Icon(
                        imageVector = Icons.Outlined.Pause,
                        contentDescription = "Pause",
                        modifier = Modifier
                            .padding(12.dp)
                            .size(42.dp)
                    )
                    PlaybackIconState.Paused -> Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier
                            .padding(12.dp)
                            .size(42.dp)
                    )
                }
            }
        }

        // Next
        IconButton(
            onClick = onNextTrack,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Outlined.SkipNext, contentDescription = "Next track")
        }

        FilledTonalIconToggleButton(
            checked = isRepeating,
            onCheckedChange = { onRepeatMode() },
            modifier = Modifier.size(42.dp),
            shape = MaterialShapes.Cookie6Sided.toShape(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = "Repeat mode"
            )
        }
    }
}
@Composable
fun Lyrics(
    loadLyrics: () -> Unit,
    track: Track?,
    lyrics: List<SyncedLyric>,
    currentLyric: SyncedLyric?,
    seekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val activeIndex = lyrics.indexOf(currentLyric)

    fun scrollToActive(idx: Int) {
        if (idx < 0) { scope.launch { listState.animateScrollToItem(0) }; return }
        val offset = (listState.layoutInfo.viewportEndOffset * 0.38f).toInt().coerceAtLeast(0)
        scope.launch { listState.animateScrollToItem(idx, scrollOffset = offset) }
    }

    LaunchedEffect(track?.id) {
        loadLyrics()
        scrollToActive(lyrics.indexOf(currentLyric))
    }

    LaunchedEffect(currentLyric?.timeMs, lyrics.hashCode()) {
        scrollToActive(activeIndex)
    }

    if (lyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "♪",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
                )
                Text(
                    text = "No lyrics available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        // Generous padding so the first and last lines can scroll to center
        contentPadding = PaddingValues(vertical = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(
            lyrics,
            key = { index, item -> item.timeMs.hashCode() * 31 + index }
        ) { index, line ->
            val distance = if (activeIndex >= 0) kotlin.math.abs(index - activeIndex) else Int.MAX_VALUE
            LyricLine(
                line = line,
                isActive = index == activeIndex,
                distance = distance,
                onClick = { seekTo(line.timeMs) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LyricLine(
    line: SyncedLyric,
    isActive: Boolean,
    distance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetAlpha = when {
        isActive      -> 1.00f
        distance == 1 -> 0.65f
        distance == 2 -> 0.38f
        else          -> 0.18f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 280),
        label = "lyricAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isActive      -> 1.00f
            distance == 1 -> 0.96f
            distance == 2 -> 0.93f
            else          -> 0.90f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lyricScale"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "lyricPillAlpha"
    )

    var tapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (tapped) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "lyricTapScale"
    )
    LaunchedEffect(tapped) {
        if (tapped) { delay(80); tapped = false }
    }

    val verticalPadDp by animateFloatAsState(
        targetValue = if (isActive) 14f else 5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lyricVertPad"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale * tapScale
                scaleY = scale * tapScale
            }
            .padding(horizontal = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer
                    .copy(alpha = pillAlpha * 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable {
                tapped = true
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = verticalPadDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = line.text,
                style = when {
                    isActive      -> MaterialTheme.typography.headlineSmall
                    distance == 1 -> MaterialTheme.typography.titleLarge
                    distance == 2 -> MaterialTheme.typography.titleMedium
                    else          -> MaterialTheme.typography.bodyLarge
                },
                fontWeight = when {
                    isActive      -> FontWeight.ExtraBold
                    distance == 1 -> FontWeight.SemiBold
                    distance == 2 -> FontWeight.Normal
                    else          -> FontWeight.Light
                },
                color = if (isActive)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isActive) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}