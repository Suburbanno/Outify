package cc.tomko.outify.ui.components.rows

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.ui.notifications.InAppNotificationController
import cc.tomko.outify.ui.notifications.NotificationSpec
import cc.tomko.outify.utils.SharedElementKey

@Composable
fun SharedTransitionScope.SwipeableTrackRow(
    track: Track,
    modifier: Modifier = Modifier,
    currentTrack: Track? = null,
    isTransitioning: Boolean = false,
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    favoriteTrack: ((String) -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SwipeableRowWithGestures(
        endGestures = listOf(
            SwipeGesture(
                thresholdFraction = 0.05f,
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddToPhotos,
                        contentDescription = "Add to queue",
                        modifier = Modifier.fillMaxSize()
                    )
                },
                onTrigger = {
                    InAppNotificationController.show(
                        NotificationSpec(
                            message = "Added to queue",
                            icon = {
                                Icon( Icons.Default.Queue, contentDescription = "Queue")
                            }
                        )
                    )
                    Spirc.addToQueue(track.uri)
                },
                backgroundColor = Color(0xC43C8C52)
            ),
            SwipeGesture(
                thresholdFraction = 0.45f,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        modifier = Modifier.fillMaxSize()
                    )
                },
                onTrigger = {  },
            ),
        ),
        startGestures = listOf(
            SwipeGesture(
                thresholdFraction = 0.25f,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Add to favorite",
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                onTrigger = { favoriteTrack?.invoke(track.uri) },
                backgroundColor = Color(0xC43C8C52),
            )
        ),
        modifier = modifier
    ) {
        TrackRow(
            title = track.name,
            artists = track.artists,
            artworkUrl = (ALBUM_COVER_URL + track.album?.getCover(CoverSize.SMALL)?.uri),
            isPlaying = currentTrack?.uri.equals(track.uri),
            isSelected = false,
            trailingContent = trailingContent,
            onRowClick = onRowClick,
            onRowLongClick = onRowLongClick,
            onArtworkClick = onArtworkClick,
            onTitleClick = onTitleClick,
            onArtistClick = onArtistClick,
            sharedTransitionKey = if (isTransitioning)
                "${SharedElementKey.ALBUM_ARTWORK}_${track.uri}" else null
        )
    }
}