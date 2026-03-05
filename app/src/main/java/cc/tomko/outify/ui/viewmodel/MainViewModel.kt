package cc.tomko.outify.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.lifecycle.ViewModel
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.setting.GestureAction
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.Side
import cc.tomko.outify.data.setting.SwipeActionHandler
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.notifications.InAppNotificationController
import cc.tomko.outify.ui.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackStateHolder: PlaybackStateHolder,
    private val spirc: SpircWrapper,
    private val settingsRepository: SettingsRepository,
): ViewModel() {
    val swipeSettings: Flow<List<GestureSetting>> =
        settingsRepository.interfaceSettings.map { it.gestureSettings }

    val swipeActionHandler = object : SwipeActionHandler {
        override fun addToQueue(uri: String) {
            spirc.addToQueue(uri)
            InAppNotificationController.show("Added to queue", { Icon(Icons.Default.Queue, contentDescription = "Added to queue") }, 1000L)
        }
        override fun startRadio(track: Track) {
            spirc.startRadio(track.uri, false)
            playbackStateHolder.setTrack(track)
            InAppNotificationController.show("Radio started", { Icon(Icons.Default.Radio, contentDescription = "Radio started") }, 1000L)
        }
        override fun favorite(trackUri: String) { }
        override fun addToPlaylist(track: Track) { }
    }

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

}