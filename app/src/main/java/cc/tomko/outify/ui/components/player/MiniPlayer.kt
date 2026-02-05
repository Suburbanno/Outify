
package cc.tomko.outify.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.utils.SharedElementKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.MiniPlayer(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val playbackState = OutifyApplication.playbackManager.playbackStateHolder
    val currentTrack by playbackState.currentTrack.collectAsState()
    val isPlaying by playbackState.isPlaying.collectAsState()

    val currentTime by playbackState.positionMs.collectAsState()
    val totalTime = currentTrack?.duration

    val imageSize = 40.dp
    val imageSizePx = with(LocalDensity.current) { imageSize.roundToPx() }
    val artworkUrl = currentTrack ?.album ?.covers ?.firstOrNull() ?.uri ?.let { OutifyApplication.ALBUM_COVER_URL + it }

    val imageRequest = remember(artworkUrl, imageSizePx) {
        ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(imageSizePx)
            .allowHardware(true)
            .build()
    }
    val imageLoader = (LocalContext.current.applicationContext as OutifyApplication).imageLoader

    AnimatedVisibility(
        visible = true,
    ) {
        Surface(
            tonalElevation = 3.dp,
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    backStack.add(Route.PlayerScreen)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp)
            ) {
                // Album artwork
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                        .size(imageSize)
                        .sharedElementWithCallerManagedVisibility(
                            sharedContentState = rememberSharedContentState(SharedElementKey.PLAYER_ARTWORK),
                            visible = true,
                        )
                ) {
                    AsyncImage(
                        model = imageRequest,
                        imageLoader = imageLoader,
                        contentDescription = "Artwork",
                        modifier = Modifier
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(imageSize)
                ) {
                    // Title | Artist row
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

                    // Elapsed time / total time
                    Row {
                        Text(
                            text = "${formatTime(currentTime)} / ${formatTime(totalTime ?: 0L)}",
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            maxLines = 1,
                        )
                    }
                }

                Surface(
                    tonalElevation = 10.dp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            OutifyApplication.spirc.playerPrevious()
                        }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null
                            )
                        }

                        IconButton(onClick = {
                            OutifyApplication.spirc.playerPlayPause()
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

                        IconButton(onClick = {
                            OutifyApplication.spirc.playerNext()
                        }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null
                            )
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
