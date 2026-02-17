package cc.tomko.outify.ui.viewmodel.player

import androidx.lifecycle.ViewModel
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
): ViewModel() {
    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun isPlaying(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isPlaying }

    fun currentTime(): Flow<Long> =
        playbackStateHolder.state.map { it.position.active.inWholeMilliseconds }

    fun setTrack(track: Track?) {
        playbackStateHolder.setTrack(track)
    }
}