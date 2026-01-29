package cc.tomko.outify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.model.PlayerAction
import cc.tomko.outify.ui.model.PlayerUIState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(
    playbackStateHolder: PlaybackStateHolder
): ViewModel() {
    val currentTrack: StateFlow<Track?> = playbackStateHolder.currentTrack

    /**
     * Holds the UI state, which extracts data from various available sources.
     */
    val uiState: StateFlow<PlayerUIState> =
        currentTrack.map { track ->
            PlayerUIState(
                title = track?.name ?: "Unknown Track",
                artist = track?.artists?.joinToString { it.name } ?: "Unknown Artist",
                albumArt = track?.album?.covers?.firstOrNull()?.uri,
                isPlaying = track != null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUIState())

    fun onAction(action: PlayerAction) {
        val spirc = OutifyApplication.spirc
        when (action) {
            PlayerAction.PlayPause -> spirc.playerPlayPause()
            PlayerAction.Next -> spirc.playerNext()
            PlayerAction.Previous -> spirc.playerPrevious()
        }
    }
}