package cc.tomko.outify.ui.components.bottomsheet

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.SmartImage
import cc.tomko.outify.ui.notifications.InAppNotificationController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoBottomSheet(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,

    onArtworkClick: (() -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onOpenAlbum: (() -> Unit)? = null,
    onOpenArtist: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onSaveToPlaylist: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onCopyUri: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val imageSize = 96.dp

    val artworkUrl = remember(track) { ALBUM_COVER_URL + (track.album?.getCover(CoverSize.MEDIUM)?.uri ?: "") }

    val defaultShare: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/track/${track.id}")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share track"))
    }

    val defaultCopy: () -> Unit = {
        clipboardManager.setText(AnnotatedString("https://open.spotify.com/track/${track.id}"))
        InAppNotificationController.show("Copied to clipboard")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: artwork + title + artists + share/copy icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .size(imageSize)
                        .clickable {
                            onArtworkClick?.invoke()
                            onDismiss()
                        }
                ) {
                    SmartImage(
                        url = artworkUrl,
                        contentDescription = "Artwork",
                        modifier = Modifier.fillMaxSize(),
                        monochrome = LocalUiSettings.current.monochromeTracks
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )

                    val artists = track.artists

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        artists.forEachIndexed { index, artist ->

                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.clickable {
                                    onArtistClick?.invoke(artist)
                                    onDismiss()
                                }
                            )

                            if (index < artists.lastIndex) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Share icon
                IconButton(
                    onClick = { onShare?.invoke() ?: defaultShare() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }

                // Copy icon
                IconButton(
                    onClick = { onCopyUri?.invoke() ?: defaultCopy() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy link")
                }
            }

            // album / artist / queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open album
                FilledTonalButton(
                    onClick = {
                        onOpenAlbum?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Queue, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open album")
                }

                OutlinedButton(
                    onClick = {
                        onOpenArtist?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open artist")
                }
            }

            // add/save/like/radio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        onAddToQueue?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Queue, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to queue")
                }

                FilledTonalButton(
                    onClick = {
                        onSaveToPlaylist?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            // like / radio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = {
                        onToggleLike?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Like")
                }

                OutlinedButton(
                    onClick = {
                        onStartRadio?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Radio, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start radio")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}