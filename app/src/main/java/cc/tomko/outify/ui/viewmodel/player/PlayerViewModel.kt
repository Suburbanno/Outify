package cc.tomko.outify.ui.viewmodel.player

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.model.PlaybackState
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.model.player.PlayerUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(
    playbackStateHolder: PlaybackStateHolder
): ViewModel() {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    val currentTrack: StateFlow<Track?> =
        state
            .map { it.currentTrack }
            .stateIn(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val uiState: StateFlow<PlayerUIState> =
        playbackStateHolder.state
            .map { state ->
                val track = state.currentTrack
                val position = state.position
                PlayerUIState(
                    title = track?.name ?: "Unknown Track",
                    artist = track?.artists?.joinToString { it.name } ?: "Unknown Artist",
                    albumArt = track?.album?.covers?.firstOrNull()?.uri,
                    isPlaying = state.isPlaying,
                    isExplicit = track?.explicit ?: false,
                    totalLengthMs = track?.duration ?: 0L,
                    positionMs = position.active.inWholeMilliseconds,
                    lastUpdateTime = position.lastSync,
                )
            }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5_000), PlayerUIState())

    /**
     * On Player UI action - like play/pause/..
     */
    fun onAction(action: PlayerAction) {
        val spirc = OutifyApplication.spirc
        when (action) {
            PlayerAction.PlayPause -> spirc.playerPlayPause()
            PlayerAction.Next -> spirc.playerNext()
            PlayerAction.Previous -> spirc.playerPrevious()
        }
    }
}