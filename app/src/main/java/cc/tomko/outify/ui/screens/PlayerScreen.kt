package cc.tomko.outify.ui.screens

import android.os.SystemClock
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel
import cc.tomko.outify.ui.viewmodel.factory.PlayerViewModelFactory
import cc.tomko.outify.utils.SharedElementKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.PlayerScreen(playbackStateHolder: PlaybackStateHolder) {
    val context = LocalContext.current

    val vm: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(playbackStateHolder)
    )

    val uiState by vm.uiState.collectAsState()
    val artworkUrl = uiState.albumArt?.let { ALBUM_COVER_URL + it }
    println(artworkUrl)

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
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = uiState.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Time seeker
            TrackProgressBar(
                durationMs = uiState.totalLengthMs,
                positionMs = uiState.positionMs,
                lastSyncMs = uiState.lastUpdateTime,
                isPlaying = uiState.isPlaying,
            )

            Spacer(Modifier.height(32.dp))

            PlaybackControls(
                isPlaying = uiState.isPlaying,
                onPlayPause = { vm.onAction(PlayerAction.PlayPause) },
                onNextTrack = { vm.onAction(PlayerAction.Next) },
                onPreviousTrack = { vm.onAction(PlayerAction.Previous) },
            )
        }
    }
}

@Composable
fun TrackProgressBar(
    durationMs: Long,
    positionMs: Long,
    lastSyncMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit = {}
) {
    var displayedPosition by remember { mutableLongStateOf(positionMs.coerceIn(0L, durationMs)) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(positionMs, lastSyncMs, isPlaying) {
        if (!isDragging) {
            displayedPosition = if (isPlaying) {
                val now = SystemClock.elapsedRealtime()
                val played = positionMs + (now - lastSyncMs)
                played.coerceIn(0L, durationMs)
            } else {
                positionMs.coerceIn(0L, durationMs)
            }
        }
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                displayedPosition = (positionMs + (now - lastSyncMs)).coerceIn(0L, durationMs)
                delay(250L)
            }
        }
    }

    val durationSec = (durationMs / 1000f).coerceAtLeast(1f)
    val valueSec = displayedPosition / 1000f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "${formatTime(displayedPosition)} / ${formatTime(durationMs)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = valueSec,
            onValueChange = { newValueSec ->
                isDragging = true
                displayedPosition = (newValueSec * 1000f).toLong().coerceIn(0L, durationMs)
            },
            onValueChangeFinished = {
                onSeek(displayedPosition)
                // TODO: Send seek to spotify
                isDragging = false
            },
            valueRange = 0f..durationSec,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}


@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        ExpressiveFloatingActionButton(
            icon = Icons.Outlined.SkipPrevious,
            checked = true,
            checkedColor = MaterialTheme.colorScheme.secondaryContainer,
            uncheckedColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
                onPreviousTrack()
            }
        )

        // Play/Pause button
        val icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow
        FilledIconButton(
            onClick = { onPlayPause() },
            shape = CircleShape,
            modifier = Modifier.size(96.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.padding(12.dp).size(42.dp)
            )
        }

        // Skip
        ExpressiveFloatingActionButton(
            icon = Icons.Outlined.SkipNext,
            checked = true,
            checkedColor = MaterialTheme.colorScheme.secondaryContainer,
            uncheckedColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
                onNextTrack()
            },
        )
    }
}

@Composable
fun RowScope.ExpressiveFloatingActionButton(
    icon: ImageVector,
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    onClick: () -> Unit,
) {
    var shapeSelected by remember { mutableStateOf(false) }

    val cornerRadius = if (shapeSelected) 6.dp else 16.dp

    val iconSize = 70.dp
    IconButton(
        modifier = Modifier
            .padding(4.dp)
            .weight(.75f)
            .height(iconSize),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (shapeSelected || checked) checkedColor else uncheckedColor,
            contentColor = if (shapeSelected || checked) uncheckedColor else checkedColor,
        ),
        shape = MaterialTheme.shapes.large.copy(CornerSize(cornerRadius)),
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
    }
}