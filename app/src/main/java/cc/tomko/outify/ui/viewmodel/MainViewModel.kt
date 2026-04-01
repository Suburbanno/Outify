package cc.tomko.outify.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.toSpotifyUri
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.SwipeActionHandler
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.notifications.InAppNotificationController
import cc.tomko.outify.data.repository.InterfaceSettings
import cc.tomko.outify.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackStateHolder: PlaybackStateHolder,
    private val spirc: SpircWrapper,
    private val settingsRepository: SettingsRepository,
    private val spClient: SpClient,
): ViewModel() {
    val swipeSettings: Flow<List<GestureSetting>> =
        settingsRepository.interfaceSettings.map { it.gestureSettings }

    val interfaceSettings: Flow<InterfaceSettings> =
        settingsRepository.interfaceSettings

    val swipeActionHandler = object : SwipeActionHandler {
        override fun addToQueue(uri: String) {
            this@MainViewModel.addToQueue(uri)
        }
        override fun startRadio(track: Track) {
            this@MainViewModel.startRadio(track)
        }
        override fun favorite(trackUri: String) {
            this@MainViewModel.favorite(trackUri)
        }
        override fun addToPlaylist(track: Track) { }
        override fun trackInfo(track: Track) { openTrackInfo(track) }
    }

    val currentTrack: StateFlow<Track?> = playbackStateHolder.state
        .map { it.currentTrack }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun addToQueue(uri: String) {
        spirc.addToQueue(uri)
        InAppNotificationController.show("Added to queue", { Icon(Icons.Default.Queue, contentDescription = "Added to queue") }, 1000L)
    }

    fun startRadio(track: Track) {
        spirc.startRadio(track.toSpotifyUri(), false)
        playbackStateHolder.setTrack(track)
        InAppNotificationController.show("Radio started", { Icon(Icons.Default.Radio, contentDescription = "Radio started") }, 1000L)
    }

    fun favorite(trackUri: String) {
        spClient.saveItems(arrayOf(trackUri))
    }

    fun openTrackInfo(track: Track) {
        viewModelScope.launch {
            GlobalPopupController.showTrackPopup(track)
        }
    }
}