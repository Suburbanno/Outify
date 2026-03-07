package cc.tomko.outify.ui.viewmodel.library

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.dao.LikedDao
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.playback.PlaybackStateHolder
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

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val metadata: Metadata,
    private val spClient: SpClient,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
    val likedDao: LikedDao,
): ViewModel() {
    val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState

    private val currentArtistId = MutableStateFlow<String?>(null)

    val likedTrackUris: StateFlow<Set<String>> = currentArtistId
        .flatMapLatest { artistId ->
            if (artistId.isNullOrEmpty()) {
                flowOf(emptySet())
            } else {
                likedDao.observeLikedUrisByArtist(artistId)
                    .map { it.toHashSet() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )
    private val popularTrackUris = MutableStateFlow<List<String>>(emptyList())
    private val albumUris = MutableStateFlow<List<String>>(emptyList())

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun isPlaying(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isPlaying }

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedTracks: StateFlow<List<Track>> = likedTrackUris
        .flatMapLatest { uris ->
            if (uris.isEmpty()) flowOf(emptyList())
            else metadata.observeTracks(uris.toList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val popularTracks: StateFlow<List<Track>> = popularTrackUris
        .flatMapLatest { uris ->
            if (uris.isEmpty()) flowOf(emptyList())
            else metadata.observeTracks(uris)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val albums: StateFlow<List<Album>> = albumUris
        .flatMapLatest { uris ->
            if(uris.isEmpty()) flowOf(emptyList())
            else metadata.observeAlbums(uris)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun loadArtist(artistUri: String) {
        viewModelScope.launch {
            try {
                val artist = metadata.getArtistMetadata(artistUri)
                if(artist == null) {
                    _uiState.value = ArtistUiState.Error("Artist failed to fetch")
                    return@launch
                }

                popularTrackUris.value = artist.tracks
                albumUris.value = artist.albums

                currentArtistId.value = artist.id
                _uiState.value = ArtistUiState.Success(artist)
            } catch (e: Exception) {
                _uiState.value = ArtistUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }
}

sealed interface ArtistUiState {
    object Loading : ArtistUiState
    data class Success(val artist: Artist) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}
