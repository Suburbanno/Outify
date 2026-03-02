package cc.tomko.outify.ui.viewmodel

import androidx.lifecycle.ViewModel
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackStateHolder: PlaybackStateHolder
): ViewModel() {
    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }
}