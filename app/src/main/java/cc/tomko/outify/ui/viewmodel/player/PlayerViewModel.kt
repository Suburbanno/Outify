package cc.tomko.outify.ui.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Lyrics
import cc.tomko.outify.data.SyncedLyric
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.model.PlaybackState
import cc.tomko.outify.ui.model.player.PlayerAction
import cc.tomko.outify.ui.model.player.PlayerUIState
import cc.tomko.outify.ui.repository.PlayerRepository
import cc.tomko.outify.ui.repository.SettingsRepository
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val spirc: SpircWrapper,
    val imageLoader: ImageLoader,
    private val playerRepository: PlayerRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    private val settingsRepository: SettingsRepository,
): ViewModel() {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _lyrics = MutableStateFlow<List<SyncedLyric>>(emptyList())
    val lyrics: StateFlow<List<SyncedLyric>> = _lyrics

    private val _positionMs = MutableStateFlow(playbackStateHolder.estimatePosition().inWholeMilliseconds)
    val positionMs = _positionMs.asStateFlow()

    val isShuffling = settingsRepository.shuffleEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isRepeating = settingsRepository.repeatEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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
            PlayerAction.PlayPause -> {
                spirc.playerPlayPause()
                viewModelScope.launch {
                    playbackStateHolder.setPlaying(!playbackStateHolder.state.value.isPlaying)
                }
            }
            PlayerAction.Next -> spirc.playerNext()
            PlayerAction.Previous -> {
                spirc.playerPrevious()
                viewModelScope.launch {
                    playbackStateHolder.seekTo(Duration.ZERO)
                }
            }
            is PlayerAction.SeekTo -> {
                viewModelScope.launch {
                    playbackStateHolder.seekTo(action.position.toDuration(DurationUnit.MILLISECONDS))
                    spirc.seekTo(action.position)
                }
            }
            PlayerAction.RepeatToggle -> {
                val newValue = !isRepeating.value
                viewModelScope.launch {
                    settingsRepository.setRepeat(newValue)
                    spirc.repeat(newValue)
                }
            }
            PlayerAction.ShuffleToggle -> {
                val newValue = !isShuffling.value
                viewModelScope.launch {
                    settingsRepository.setShuffle(newValue)
                    spirc.shuffle(newValue)
                }
            }
        }
    }

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun loadLyrics(){
        viewModelScope.launch {
            val track = playbackStateHolder.state.value.currentTrack
            val result = playerRepository.getLyrics(track)
            _lyrics.value = result
        }
    }

    val currentLyric: StateFlow<SyncedLyric?> =
        combine(lyrics, positionMs) { lyrics, position ->
            lyrics.lastOrNull { it.timeMs <= position }

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null
        )
}