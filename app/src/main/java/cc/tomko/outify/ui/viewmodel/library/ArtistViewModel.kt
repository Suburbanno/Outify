package cc.tomko.outify.ui.viewmodel.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val metadata: Metadata,
): ViewModel() {
    val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState

    private val likedTrackUris = MutableStateFlow<List<String>>(emptyList())
    private val popularTrackUris = MutableStateFlow<List<String>>(emptyList())

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

    fun loadArtist(artistUri: String) {
        viewModelScope.launch {
            try {
                val artist = metadata.getArtistMetadata(artistUri)
                if(artist == null) {
                    _uiState.value = ArtistUiState.Error("Artist failed to fetch")
                    return@launch
                }

                val likedSongsJson = OutifyApplication.session.spClient.getUserCollection(":artist:${artist.id}")
                val likedSongsUris = json.decodeFromString<List<String>>(likedSongsJson)

                likedTrackUris.value = likedSongsUris
                popularTrackUris.value = artist.tracks

                _uiState.value = ArtistUiState.Success(artist, likedSongsUris)
            } catch (e: Exception) {
                _uiState.value = ArtistUiState.Error(e.message ?: "An error occurred")
            }
        }
    }
}

sealed interface ArtistUiState {
    object Loading : ArtistUiState
    data class Success(val artist: Artist, val likedTracks: List<String>) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}
