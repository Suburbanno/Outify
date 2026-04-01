package cc.tomko.outify.ui.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.data.dao.LikedDao
import cc.tomko.outify.data.database.track.LikedTrackEntity
import cc.tomko.outify.data.metadata.TrackMetadataHelper
import cc.tomko.outify.playback.PlaybackStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    val spirc: SpircWrapper,
    private val playbackStateHolder: PlaybackStateHolder,
    private val trackMetadata: TrackMetadataHelper,
    private val likedDao: LikedDao,
): ViewModel() {
    private val _state = MutableStateFlow<TrackUiState?>(null)
    val uiState: StateFlow<TrackUiState?> = _state

    val currentTrack: StateFlow<Track?> = playbackStateHolder.state
        .map { it.currentTrack }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val isPlaying: StateFlow<Boolean> = playbackStateHolder.state
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun isLiked(trackId: String): StateFlow<Boolean> =
        likedDao.observeIsTrackLiked(trackId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    fun toggleLike(trackId: String) {
        viewModelScope.launch {
            if (likedDao.containsTrack(trackId)) {
                likedDao.delete(trackId)
            } else {
                likedDao.insert(
                    LikedTrackEntity(
                        trackId = trackId,
                        position = 0.0,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun loadTrack(uri: String) {
        viewModelScope.launch {
            val track = trackMetadata.getTrackMetadata(listOf(uri)).firstOrNull()
            val state = TrackUiState(
                track = track,
                error = if(track == null) "Failed to get Track" else null
            )

            _state.value = state
        }
    }
}

data class TrackUiState(
    val track: Track?,
    val error: String?,
)