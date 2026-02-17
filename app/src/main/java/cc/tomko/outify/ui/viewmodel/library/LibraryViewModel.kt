package cc.tomko.outify.ui.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.Playlist
import cc.tomko.outify.data.getCover
import cc.tomko.outify.utils.isValidSpotifyPlaylistUri
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val metadata: Metadata,
): ViewModel() {
    val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState

    private val playlistUris = MutableStateFlow<List<String>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlists: StateFlow<List<Playlist>> =
        playlistUris
            .flatMapLatest { uris ->
                if (uris.isEmpty()) {
                    flow { emit(emptyList()) }
                } else {
                    metadata.observePlaylists(uris)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    suspend fun getArtworkUrl(playlist: Playlist): String {
        if(playlist.attributes.pictureId.isNotEmpty()) {
            return ALBUM_COVER_URL + playlist.attributes.pictureId
        }

        // Getting first track
        val trackUri: String = playlist.contents.firstOrNull()?.uri ?: ""
        val track = metadata.getTrackMetadata(listOf(trackUri)).firstOrNull()

        return (ALBUM_COVER_URL + track?.album?.getCover(CoverSize.MEDIUM)?.uri)
    }

    fun loadPlaylistUris() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            runCatching {
                metadata.getPlaylistUris()
            }.onSuccess { uris ->
                playlistUris.value = uris
                _uiState.value = LibraryUiState.Success
            }.onFailure {
                _uiState.value = LibraryUiState.Error(it.message ?: "Unknown error")
            }
        }
    }

}

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    object Success : LibraryUiState
    data class Error(val error: String) : LibraryUiState
}