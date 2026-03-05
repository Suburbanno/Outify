package cc.tomko.outify.ui.components.rows

import android.annotation.SuppressLint
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import cc.tomko.outify.data.setting.buildSwipeGesturesForTrack

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SharedTransitionScope.SwipeableTrackRowConfigured(
    track: Track,
    modifier: Modifier = Modifier,

    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,

    trailingContent: @Composable (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val (start, end) = rememberTrackGestures(track)

        SwipeableRowWithGestures(
            startGestures = start,
            endGestures = end,
            modifier = Modifier
        ) {
            TrackRow(
                title = track.name,
                artists = track.artists,
                artworkUrl = (ALBUM_COVER_URL + track.album?.getCover(CoverSize.SMALL)?.uri),

                onRowClick = onRowClick,
                onRowLongClick = onRowLongClick,
                onArtistClick = onArtistClick,
                onArtworkClick = onArtworkClick,
                onTitleClick = onTitleClick,

                trailingContent = trailingContent,
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