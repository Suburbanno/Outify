package cc.tomko.outify.playback

import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.model.PlayerUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlaybackStateHolder {
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    fun onTrackChanged(track: Track) {
        _currentTrack.value = track
    }

}