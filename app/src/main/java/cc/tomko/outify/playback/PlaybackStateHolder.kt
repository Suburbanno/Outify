package cc.tomko.outify.playback

import android.os.SystemClock
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

    /**
     * Playback position in ms
     */
    val positionMs = MutableStateFlow(0L)

    /**
     * Last sync of positionMs with Spotify
     */
    val lastSync = MutableStateFlow(0L)

    /**
     * Is the track playing
     */
    val isPlaying = MutableStateFlow(false)

    /**
     * When the entire track changes
     */
    fun onTrackChanged(track: Track) {
        _currentTrack.value = track
    }

    /**
     * When the Track position updates
     */
    fun onPositionUpdate(newPosition: Long) {
        val now = SystemClock.elapsedRealtime()
        positionMs.value = newPosition
        lastSync.value = now
    }

    /**
     * Updates the currently playing status
     */
    fun onPlayStateChange(playing: Boolean) {
        isPlaying.value = playing
    }

}