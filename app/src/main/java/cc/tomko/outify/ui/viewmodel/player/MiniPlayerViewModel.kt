package cc.tomko.outify.ui.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
): ViewModel() {
    private val _positionMs = MutableStateFlow(playbackStateHolder.estimatePosition().inWholeMilliseconds)
    val positionMs = _positionMs.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _positionMs.value = playbackStateHolder.estimatePosition().inWholeMilliseconds
                delay(250L)
            }
        }
    }

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun isPlaying(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isPlaying }

    fun isBuffering(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isBuffering }

    fun setTrack(track: Track?) {
        if(track == null){
            spirc.playerPause()
        }
        playbackStateHolder.setTrack(track)
    }
}