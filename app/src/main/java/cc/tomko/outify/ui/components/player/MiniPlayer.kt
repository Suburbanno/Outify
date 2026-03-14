package cc.tomko.outify.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.getCover
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.SmartImage
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.viewmodel.player.MiniPlayerViewModel
import cc.tomko.outify.utils.SharedElementKey
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val TAB_HEIGHT = 20.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.MiniPlayer(
    viewModel: MiniPlayerViewModel,
    onDismiss: () -> Unit,
    showQueue: () -> Unit,
    modifier: Modifier = Modifier,
    onExpand: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrack().collectAsState(initial = null)
    val isPlaying by viewModel.isPlaying().collectAsState(initial = false)
    val isBuffering by viewModel.isBuffering().collectAsState(initial = false)
    val currentTime by viewModel.positionMs.collectAsState(initial = 0L)
    val spirc = viewModel.spirc

    val totalTime = currentTrack?.duration ?: 0L

    val imageSize = 40.dp
    val artworkUrl = currentTrack?.album?.getCover(CoverSize.SMALL)?.uri.let { ALBUM_COVER_URL + it }

    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier.fillMaxWidth()
            .pointerInput(Unit) {
                var totalDragX = 0f
                var totalDragY = 0f

                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        coroutineScope.launch {
                            val target = max(0f, offsetY.value + dragAmount.y)
                            offsetY.snapTo(target)
                        }
                    },
                    onDragEnd = {
                        val horizontalThreshold = 100.dp.toPx()
                        val verticalThreshold = 40.dp.toPx()

                        when {
                            totalDragX > horizontalThreshold -> spirc.playerPrevious()
                            totalDragX < -horizontalThreshold -> spirc.playerNext()
                        }

                        if (totalDragY > verticalThreshold) {
                            onDismiss()
                        }

                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 300)
                            )
                        }

                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 300)
                            )
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    }
                )
            }
            .offset { IntOffset(x = 0, y = offsetY.value.roundToInt()) },
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = onExpand != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .wrapContentWidth()
                .offset(y = -TAB_HEIGHT, x = (-55).dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .height(TAB_HEIGHT)
                    .clickable { onExpand?.invoke() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Expand",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Main card
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick?.invoke() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp)
            ) {
                // Album artwork
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .size(imageSize)
                            .sharedElementWithCallerManagedVisibility(
                                sharedContentState = rememberSharedContentState(SharedElementKey.PLAYER_ARTWORK),
                                visible = true,
                            )
                    ) {
                        SmartImage(
                            url = artworkUrl,
                            contentDescription = "Artwork",
                            modifier = Modifier
                                .fillMaxSize(),
                            monochrome = LocalUiSettings.current.monochromePlayer
                        )
                    }

                    if (isBuffering) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(imageSize)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentTrack?.name ?: "-----",
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = currentTrack?.artists?.joinToString { it.name } ?: "----",
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row {
                        Text(
                            text = "${formatTime(currentTime)} / ${formatTime(totalTime)}",
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            maxLines = 1,
                        )
                    }
                }

                IconButton(onClick = showQueue) {
                    Icon(Icons.Default.Menu, contentDescription = "See queue")
                }

                Surface(
                    tonalElevation = 10.dp,
                    shape = MaterialShapes.Cookie4Sided.toShape()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(onClick = {
                            viewModel.setTrack(currentTrack)
                            spirc.playerPlayPause()
                        }) {
                            if (isPlaying) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%01d:%02d".format(minutes, seconds)
}