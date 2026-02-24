package cc.tomko.outify.ui.viewmodel.player

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.model.PlaybackState
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.model.player.PlayerUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val spirc: SpircWrapper,
    private val playbackStateHolder: PlaybackStateHolder,
): ViewModel() {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(playbackStateHolder.estimatePosition().inWholeMilliseconds)
    val positionMs = _positionMs.asStateFlow()

    private val _isShuffling = MutableStateFlow(false)
    val isShuffling = _isShuffling.asStateFlow()
    private val _isRepeating = MutableStateFlow(false)
    val isRepeating = _isRepeating.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _positionMs.value = playbackStateHolder.estimatePosition().inWholeMilliseconds
                delay(250L)
            }
        }

        viewModelScope.launch {
            playbackStateHolder.state.collect { playback ->
                _state.value = playback
            }
        }
    }

    val uiState: StateFlow<PlayerUIState> =
        playbackStateHolder.state
            .map { state ->
                val track = state.currentTrack
                val position = state.position
                PlayerUIState(
                    title = track?.name ?: "Unknown Track",
                    artists = track?.artists ?: emptyList(),
                    albumArt = track?.album?.getCover(CoverSize.LARGE)?.uri,
                    isPlaying = state.isPlaying,
                    isExplicit = track?.explicit ?: false,
                    totalLengthMs = track?.duration ?: 0L,
                    positionMs = position.active.inWholeMilliseconds,
                    lastUpdateTime = position.lastSync,
                    isBuffering = state.isBuffering,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUIState())

    /**
     * On Player UI action - like play/pause/..
     */
    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.PlayPause -> spirc.playerPlayPause()
            PlayerAction.Next -> spirc.playerNext()
            PlayerAction.Previous -> spirc.playerPrevious()
            is PlayerAction.SeekTo -> {
                viewModelScope.launch {
                    spirc.seekTo(action.position)
                }
            }
            PlayerAction.RepeatToggle -> {
                _isRepeating.value = !spirc.isRepeating
                spirc.repeat(!spirc.isRepeating)
            }
            PlayerAction.ShuffleToggle -> {
                _isShuffling.value = !spirc.isShuffling
                spirc.shuffle(!spirc.isShuffling)
            }
        }
    }

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }
}