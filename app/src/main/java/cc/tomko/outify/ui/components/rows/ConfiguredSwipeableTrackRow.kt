@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package cc.tomko.outify.ui.components.rows

import android.annotation.SuppressLint
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.data.setting.LocalSwipeActionHandler
import cc.tomko.outify.data.setting.LocalSwipeGestureSettings
import cc.tomko.outify.data.setting.Side
import cc.tomko.outify.data.setting.buildLongPressAction
import cc.tomko.outify.data.setting.buildSwipeGesturesForTrack
import cc.tomko.outify.data.sharedTransitionKey

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SharedTransitionScope.SwipeableTrackRowConfigured(
    track: Track,
    modifier: Modifier = Modifier,
    currentTrack: Track? = null,

    isLiked: Boolean = false,
    isPlaybackPlaying: Boolean = false,
    isTransitioning: Boolean = false,
    isSelected: Boolean = false,

    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,

    trailingContent: @Composable (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val (start, end) = rememberTrackGestures(track)

        val settings = LocalSwipeGestureSettings.current
        val handler = LocalSwipeActionHandler.current
        val longPressAction = remember(settings, track) {
            buildLongPressAction(settings, handler, track)
        }

        SwipeableRowWithGestures(
            startGestures = start,
            endGestures = end,
            modifier = Modifier
        ) {
            TrackRow(
                title = track.name,
                artists = track.artists,
                artworkUrl = (ALBUM_COVER_URL + track.album?.getCover(CoverSize.SMALL)?.uri),
                isExplicit = track.explicit,
                isLoaded = currentTrack?.uri.equals(track.uri),
                isPlaying = isPlaybackPlaying,
                isSelected = isSelected,

                onRowClick = onRowClick,
                onRowLongClick = {
                    if(onRowLongClick != null) {
                        onRowLongClick.invoke()
                    } else  {
                        longPressAction?.invoke()
                    }
                },
                onArtistClick = onArtistClick,
                onArtworkClick = onArtworkClick,
                onTitleClick = onTitleClick,

                trailingContent = {
                    // Liked indicator
                    if(isLiked){
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Liked"
                        )
                    }

                    trailingContent?.invoke()
                },

                sharedTransitionKey = if(isTransitioning)
                    track.album?.sharedTransitionKey() else null
            )
        }
    }
}

@Composable
fun rememberTrackGestures(track: Track): Pair<List<SwipeGesture>, List<SwipeGesture>> {
    val settings = LocalSwipeGestureSettings.current
    val handler = LocalSwipeActionHandler.current

    val gestures = remember(settings, track) {
        buildSwipeGesturesForTrack(settings, handler, track)
    }

    val start = gestures.filter { it.side == Side.Start }.map { it.swipeGesture }
    val end = gestures.filter { it.side == Side.End }.map { it.swipeGesture }

    return start to end
}