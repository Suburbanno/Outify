package cc.tomko.outify.data.setting

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
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
    track: Track
): Pair<List<SwipeGesture>, List<SwipeGesture>> {
    val start = mutableListOf<SwipeGesture>()
    val end = mutableListOf<SwipeGesture>()

    gestureSettings.filter { it.enabled && it.side != null }.forEach { s ->
        val threshold = (s.thresholdFraction ?: 0.25f).coerceIn(0f, 1f)
        val bgColor = s.backgroundHex?.let { Color(it) } ?: colorForAction(s.action)

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
                GestureAction.ADD_TO_FAVORITE -> Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.fillMaxSize())
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

private fun colorForAction(action: GestureAction): Color = when(action) {
    GestureAction.ADD_TO_QUEUE -> Color(0xFF4CAF50) // green
    GestureAction.PLAY_NEXT -> Color(0xFFF44336)
    GestureAction.START_RADIO -> Color(0xFFFFC107) // amber
    GestureAction.ADD_TO_PLAYLIST -> Color(0xFF2196F3) // blue
    GestureAction.ADD_TO_FAVORITE -> Color(0xFFE91E63) // pink
    GestureAction.SHOW_TRACK_INFO -> Color(0xFF9C27B0) // purple
    GestureAction.NONE -> Color.Unspecified
}