package cc.tomko.outify.ui.viewmodel.library.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.screens.library.album.AlbumUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * View model for album screen
 */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val metadata: Metadata,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
    val spClient: SpClient,
    val json: Json,
): ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val likedTrackUris = MutableStateFlow<List<String>>(emptyList())
    val uiState: StateFlow<AlbumUiState> = _uiState

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedTracks: StateFlow<List<Track>> = likedTrackUris
        .flatMapLatest { uris ->
            if (uris.isEmpty()) flowOf(emptyList())
            else metadata.observeTracks(uris)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

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

                // Load liked tracks
                val likedSongsJson = spClient.getUserCollection(":album:${album.id}")
                val likedSongsUris = json.decodeFromString<List<String>>(likedSongsJson)

                likedTrackUris.value = likedSongsUris
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