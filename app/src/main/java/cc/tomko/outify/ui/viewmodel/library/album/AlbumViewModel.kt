package cc.tomko.outify.ui.viewmodel.library.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.screens.library.album.AlbumUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for album screen
 */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val metadata: Metadata,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper
): ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun isPlaying(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isPlaying }

    suspend fun loadAlbum(albumUri: String) {
        viewModelScope.launch {
            try {
                val album = metadata
                    .getAlbumMetadata(albumUri)

                if(album == null){
                    return@launch
                }

                val trackUris: List<String> = album.tracks.map { it }

                val tracks: List<Track> = if (trackUris.isNotEmpty()) {
                    metadata.getTrackMetadata(trackUris)
                } else emptyList()

                _uiState.value = AlbumUiState(
                    isLoading = false,
                    album = album,
                    tracks = tracks,
                )
            } catch (e: Exception) {
                _uiState.value = AlbumUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    suspend fun loadAlbumFromTrackUri(trackUri: String) {
        viewModelScope.launch {
            val albumId = metadata.getTrackAlbumId(trackUri) ?: // Throw some error?
            return@launch

            loadAlbum("spotify:album:$albumId")
        }
    }

    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }
}