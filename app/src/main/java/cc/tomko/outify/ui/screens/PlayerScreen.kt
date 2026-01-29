package cc.tomko.outify.ui.screens

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.model.PlayerAction
import cc.tomko.outify.ui.viewmodel.PlayerViewModel
import cc.tomko.outify.ui.viewmodel.factory.PlayerViewModelFactory
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun PlayerScreen(playbackStateHolder: PlaybackStateHolder) {
    val vm: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(playbackStateHolder)
    )

    val uiState by vm.uiState.collectAsState()

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

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://i.scdn.co/image/" + uiState.albumArt)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album cover",
                contentScale = ContentScale.Fit,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).size(300.dp)
//                placeholder = painterResource(R.drawable.placeholder)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = uiState.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            itemWeight = .5f,
            checked = true,
            checkedColor = MaterialTheme.colorScheme.secondaryContainer,
            uncheckedColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
                onPreviousTrack()
            }
        )

        // Play/Pause button
        ExpressiveFloatingActionButton(
            icon = Icons.Outlined.PlayArrow,
            itemWeight = 1.5f,
            checked = false,
            checkedColor = MaterialTheme.colorScheme.primaryContainer,
            uncheckedColor = MaterialTheme.colorScheme.onPrimary,
            onClick = {
                onPlayPause()
            }
        )

        // Skip
        ExpressiveFloatingActionButton(
            icon = Icons.Outlined.SkipNext,
            itemWeight = .5f,
            checked = true,
            checkedColor = MaterialTheme.colorScheme.secondaryContainer,
            uncheckedColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
                onNextTrack()
            }
        )
    }
}
@Composable
fun RowScope.ExpressiveFloatingActionButton(
    icon: ImageVector,
    itemWeight: Float,
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    onClick: () -> Unit,
) {
    var shapeSelected by remember { mutableStateOf(false) }
    val animatedRadius by animateDpAsState(
        targetValue = if (shapeSelected) 6.dp else 16.dp,
        label = "animatedRadius"
    )
    val animatedWeight by animateFloatAsState(
        targetValue = if (shapeSelected) 0.25f else 0f
    )
    val iconSize = 70.dp
    IconButton(
        modifier = Modifier
            .padding(4.dp)
            .weight(itemWeight + animatedWeight)
            .height(iconSize)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { pointerInputChange ->
                            shapeSelected = pointerInputChange.pressed
                        }
                    }
                }
            },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (shapeSelected || checked) checkedColor else uncheckedColor,
            contentColor = if (shapeSelected || checked) uncheckedColor else checkedColor,
        ),
        shape = MaterialTheme.shapes.large.copy(CornerSize(animatedRadius)),
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
    }
}