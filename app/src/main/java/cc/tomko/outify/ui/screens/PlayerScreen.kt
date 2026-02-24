package cc.tomko.outify.ui.screens

import android.os.SystemClock
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.outlined.DoubleArrow
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Artist
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.components.WavyMusicSlider
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel
import cc.tomko.outify.utils.SharedElementKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.PlayerScreen(
    viewModel: PlayerViewModel,
    onArtistClick: (Artist) -> Unit,
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val artworkUrl = uiState.albumArt?.let { ALBUM_COVER_URL + it }
    val positionMs by viewModel.positionMs.collectAsState()

    val isShuffling by viewModel.isShuffling.collectAsState()
    val isRepeating by viewModel.isRepeating.collectAsState()

    val imageSize = 400.dp
    val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx() }

    val imageRequest = remember(artworkUrl) {
        ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(imageSizePx)
            .allowHardware(true)
            .build()
    }
    val imageLoader = (LocalContext.current.applicationContext as OutifyApplication).imageLoader

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Album artwork
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    .size(imageSize)
                    .sharedBounds(
                        rememberSharedContentState(SharedElementKey.PLAYER_ARTWORK),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current
                    )
            ) {
                AsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentDescription = "Artwork",
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(uiState.isExplicit) {
                    Icon(
                        imageVector = Icons.Filled.Explicit,
                        contentDescription = "Explicit",
                        modifier = Modifier.size(32.dp),
                    )
                }

                Text(
                    text = uiState.title,
                    overflow = TextOverflow.Ellipsis,
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
                        modifier = Modifier
                            .then(
                                Modifier.combinedClickable(
                                    onClick = { onArtistClick(artist) },
                                    onLongClick = {}
                                )
                            )
                    )

                    // Add comma separator except after last
                    if (index < uiState.artists.lastIndex) {
                        Text(
                            text = ", ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Time seeker
            TrackProgressBar(
                durationMs = uiState.totalLengthMs,
                positionMs = positionMs,
                isPlaying = uiState.isPlaying,
                onSeek = { position ->
                    viewModel.onAction(PlayerAction.SeekTo(position))
                }
            )

            Spacer(Modifier.height(32.dp))

            PlaybackControls(
                isPlaying = uiState.isPlaying,
                isShuffling = isShuffling,
                isRepeating = isRepeating,
                onPlayPause = { viewModel.onAction(PlayerAction.PlayPause) },
                onNextTrack = { viewModel.onAction(PlayerAction.Next) },
                onPreviousTrack = { viewModel.onAction(PlayerAction.Previous) },
                onShuffleChange = { viewModel.onAction(PlayerAction.ShuffleToggle) },
                onRepeatMode = { viewModel.onAction(PlayerAction.RepeatToggle) },
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
            sliderValue = (positionMs.toFloat() / durationMs.toFloat())
                .coerceIn(0f, 1f)
        }
    }

    val displayedPosition = (sliderValue * durationMs)
        .toLong()
        .coerceIn(0L, durationMs)

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
                val newPositionMs =
                    (sliderValue * durationMs).toLong()
                        .coerceIn(0L, durationMs)

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
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
        // Shuffle button
        IconButton(
            onClick = onShuffleChange,
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                imageVector = if (isShuffling)
                    Icons.Outlined.Shuffle
                else Icons.AutoMirrored.Outlined.Forward,
                contentDescription = "Shuffle mode",
                tint = if (isShuffling)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Previous button
        IconButton(
            onClick = onPreviousTrack,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Outlined.SkipPrevious, contentDescription = "Previous track")
        }

        // Play/Pause button
        val icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow
        var rotated by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(
            targetValue = if (rotated) 20f else 0f,
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing
            ),
            label = "playButtonRotation"
        )

        LaunchedEffect(rotated) {
            if (rotated) {
                delay(150)
                rotated = false
            }
        }

        FilledIconButton(
            onClick = {
                rotated = true
                onPlayPause()
            },
            shape = MaterialShapes.Cookie9Sided.toShape(),
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    rotationZ = rotation
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .padding(12.dp)
                    .size(42.dp)
            )
        }

        // Next track
        IconButton(
            onClick = onNextTrack,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Outlined.SkipNext, contentDescription = "Next track")
        }

        // Repeat mode
        IconButton(
            onClick = onRepeatMode,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = if(isRepeating)
                    Icons.Outlined.Repeat
                else Icons.Outlined.Loop,
                contentDescription = "Repeat mode",
                tint = if (isRepeating)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}