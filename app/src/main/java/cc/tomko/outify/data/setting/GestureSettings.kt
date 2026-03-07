package cc.tomko.outify.data.setting

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.components.rows.SwipeGesture
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class GestureSetting(
    val action: GestureAction,
    val side: Side = Side.End,
    val trigger: GestureTrigger = GestureTrigger.SwipeEnd,
    val enabled: Boolean = true,
    val thresholdFraction: Float? = null,
    val backgroundHex: Long? = null,
)

@Serializable
enum class GestureAction {
    ADD_TO_QUEUE,
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
    fun startRadio(track: Track)
    fun favorite(trackUri: String)
    fun addToPlaylist(track: Track)
    fun trackInfo(track: Track)
}

val LocalSwipeGestureSettings = compositionLocalOf<List<GestureSetting>> { emptyList() }
val LocalSwipeActionHandler = compositionLocalOf<SwipeActionHandler> {
    object : SwipeActionHandler {
        override fun addToQueue(uri: String) {}
        override fun startRadio(track: Track) {}
        override fun favorite(trackUri: String) {}
        override fun addToPlaylist(track: Track) {}
        override fun trackInfo(track: Track) {}
    }
}

data class BuiltGesture(val side: Side, val swipeGesture: SwipeGesture)

fun buildSwipeGesturesForTrack(
    gestureSettings: List<GestureSetting>,
    actionHandler: SwipeActionHandler,
    track: Track
): List<BuiltGesture> {
    return gestureSettings.filter { it.enabled }.mapNotNull { s ->
        val threshold = s.thresholdFraction ?: 0.25f

        val bgColor = s.backgroundHex?.let { Color(it) } ?: Color.Unspecified

        val onTrigger = when (s.action) {
            GestureAction.ADD_TO_QUEUE -> { { actionHandler.addToQueue(track.uri) } }
            GestureAction.START_RADIO -> { { actionHandler.startRadio(track) } }
            GestureAction.ADD_TO_FAVORITE -> { { actionHandler.favorite(track.uri) } }
            GestureAction.ADD_TO_PLAYLIST -> { { actionHandler.addToPlaylist(track) } }
            GestureAction.SHOW_TRACK_INFO -> { { actionHandler.trackInfo(track); println("track info") } }
            else -> null
        } ?: return@mapNotNull null

        val icon: @Composable (BoxScope.() -> Unit) = {
            when (s.action) {
                GestureAction.ADD_TO_QUEUE -> Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.START_RADIO -> Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.ADD_TO_FAVORITE -> Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.ADD_TO_PLAYLIST -> Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.fillMaxSize())
                GestureAction.SHOW_TRACK_INFO -> Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.fillMaxSize())
                else -> Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }

        val gesture = SwipeGesture(
            thresholdFraction = threshold.coerceIn(0f, 1f),
            icon = icon,
            onTrigger = onTrigger,
            backgroundColor = bgColor
        )

        BuiltGesture(side = s.side, swipeGesture = gesture)
    }
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
        GestureAction.START_RADIO -> { { handler.startRadio(track) } }
        GestureAction.ADD_TO_FAVORITE -> { { handler.favorite(track.uri) } }
        GestureAction.ADD_TO_PLAYLIST -> { { handler.addToPlaylist(track) } }
        GestureAction.SHOW_TRACK_INFO -> { { handler.trackInfo(track) } }
        else -> null
    }
}