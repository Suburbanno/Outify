package cc.tomko.outify.data.setting

import android.R
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.ui.components.rows.SwipeGesture
import kotlinx.serialization.Serializable

@Serializable
data class GestureSetting(
    val action: GestureAction,
    val side: Side? = Side.End,
    val trigger: GestureTrigger = GestureTrigger.SwipeEnd,
    val enabled: Boolean = true,
    val thresholdFraction: Float? = null,
    val backgroundHex: Long? = null,
)

@Serializable
enum class GestureAction {
    ADD_TO_QUEUE,
    PLAY_NEXT,
    START_RADIO,
    ADD_TO_PLAYLIST,
    ADD_TO_FAVORITE,
    SHOW_TRACK_INFO,
    NONE
}

@Serializable
enum class Side { Start, End }

@Serializable
enum class GestureTrigger {
    SwipeStart,
    SwipeEnd,
    LongPress
}

interface SwipeActionHandler {
    fun addToQueue(uri: String)
    fun playNext(uri: String)
    fun startRadio(track: Track)
    fun favorite(trackUri: String)
    fun addToPlaylist(track: Track)
    fun trackInfo(track: Track)
}

val LocalSwipeGestureSettings = compositionLocalOf<List<GestureSetting>> { emptyList() }
val LocalSwipeActionHandler = compositionLocalOf<SwipeActionHandler> {
    object : SwipeActionHandler {
        override fun addToQueue(uri: String) {}
        override fun playNext(uri: String) {}
        override fun startRadio(track: Track) {}
        override fun favorite(trackUri: String) {}
        override fun addToPlaylist(track: Track) {}
        override fun trackInfo(track: Track) {}
    }
}

fun buildSwipeGesturesForTrack(
    gestureSettings: List<GestureSetting>,
    actionHandler: SwipeActionHandler,
    track: Track,
    colorScheme: ColorScheme,
    isLiked: Boolean = false,
): Pair<List<SwipeGesture>, List<SwipeGesture>> {
    val start = mutableListOf<SwipeGesture>()
    val end = mutableListOf<SwipeGesture>()

    gestureSettings.filter { it.enabled && it.side != null }.forEach { s ->
        val threshold = (s.thresholdFraction ?: 0.25f).coerceIn(0f, 1f)
        val bgColor = s.backgroundHex?.let { Color(it) } ?: colorForAction(s.action, colorScheme, isLiked)

        val onTrigger: (() -> Unit)? = when (s.action) {
            GestureAction.ADD_TO_QUEUE -> { { actionHandler.addToQueue(track.uri) } }
            GestureAction.PLAY_NEXT -> { { actionHandler.playNext(track.uri) } }
            GestureAction.START_RADIO -> { { actionHandler.startRadio(track) } }
            GestureAction.ADD_TO_FAVORITE -> { { actionHandler.favorite(track.uri) } }
            GestureAction.ADD_TO_PLAYLIST -> { { actionHandler.addToPlaylist(track) } }
            GestureAction.SHOW_TRACK_INFO -> { { actionHandler.trackInfo(track) } }
            GestureAction.NONE -> null
        }
        if (onTrigger == null) return@forEach

        val icon: @Composable BoxScope.() -> Unit = {
            when (s.action) {
                GestureAction.ADD_TO_QUEUE -> Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.PLAY_NEXT -> Icon(Icons.Default.MoveUp, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.START_RADIO -> Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.ADD_TO_FAVORITE -> {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.ThumbDown else Icons.Default.Favorite,
                        contentDescription = if (isLiked) "Remove from favorite" else "Add to favorite",
                        modifier = Modifier.fillMaxSize(),
                        tint = if (isLiked) MaterialTheme.colorScheme.error else Color.White,
                    )
                }
                GestureAction.ADD_TO_PLAYLIST -> Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.SHOW_TRACK_INFO -> Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.NONE -> Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }

        val gesture = SwipeGesture(
            thresholdFraction = threshold,
            icon = icon,
            onTrigger = onTrigger,
            backgroundColor = bgColor
        )

        when (s.side ?: Side.End) {
            Side.Start -> start += gesture
            Side.End -> end += gesture
        }
    }

    return start to end
}

fun buildLongPressAction(
    settings: List<GestureSetting>,
    handler: SwipeActionHandler,
    track: Track
): (() -> Unit)? {
    val setting = settings.firstOrNull {
        it.enabled && it.trigger == GestureTrigger.LongPress
    } ?: return null

    return when (setting.action) {
        GestureAction.ADD_TO_QUEUE -> { { handler.addToQueue(track.uri) } }
        GestureAction.PLAY_NEXT -> { { handler.playNext(track.uri) } }
        GestureAction.START_RADIO -> { { handler.startRadio(track) } }
        GestureAction.ADD_TO_FAVORITE -> { { handler.favorite(track.uri) } }
        GestureAction.ADD_TO_PLAYLIST -> { { handler.addToPlaylist(track) } }
        GestureAction.SHOW_TRACK_INFO -> { { handler.trackInfo(track) } }
        else -> null
    }
}

private fun colorForAction(action: GestureAction, colorScheme: ColorScheme, isLiked: Boolean = false): Color = when(action) {
    GestureAction.ADD_TO_QUEUE -> colorScheme.primaryContainer
    GestureAction.PLAY_NEXT -> colorScheme.secondaryContainer
    GestureAction.START_RADIO -> colorScheme.tertiaryContainer
    GestureAction.ADD_TO_PLAYLIST -> colorScheme.primary
    GestureAction.ADD_TO_FAVORITE -> if (isLiked) Color(0xC4E53935) else colorScheme.error
    GestureAction.SHOW_TRACK_INFO -> colorScheme.secondary
    GestureAction.NONE -> Color.Unspecified
}