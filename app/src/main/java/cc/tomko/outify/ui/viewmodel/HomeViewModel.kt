package cc.tomko.outify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.model.Cover
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.asInt
import cc.tomko.outify.core.model.asSize
import cc.tomko.outify.core.model.toOutifyUri
import cc.tomko.outify.data.metadata.NativeErrorHandler
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data object NotAuthenticated : HomeUiState()
    data class Success(
        val topArtists: List<TopArtist>,
        val topTracks: List<Track>,
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@Serializable
data class TopArtist(
    val uri: String,
    val name: String,
    val imageUrl: String?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val spClient: SpClient,
    private val json: Json,
    private val trackMetadataHelper: TrackMetadataHelper,
    private val spirc: SpircWrapper,
    private val playbackStateHolder: PlaybackStateHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

    init {
        loadData()
    }

    fun loadTrack(track: Track) {
        // TODO: set the context
        spirc.load(track.toOutifyUri())

        playbackStateHolder.setTrack(track)
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                val isAuthenticated = spClient.isOAuthAuthenticated()
                if (!isAuthenticated) {
                    _uiState.value = HomeUiState.NotAuthenticated
                    loadUsername()
                    return@launch
                }

                val topArtistsJson = spClient.getUserTop("artists")
                val topArtistsError = NativeErrorHandler.handleErrorJson(topArtistsJson, "top artists")
                if (topArtistsError != null) {
                    _uiState.value = HomeUiState.NotAuthenticated
                    loadUsername()
                    return@launch
                }

                val topTracksJson = spClient.getUserTop("tracks")
                val topTracksError = NativeErrorHandler.handleErrorJson(topTracksJson, "top tracks")
                if (topTracksError != null) {
                    _uiState.value = HomeUiState.NotAuthenticated
                    loadUsername()
                    return@launch
                }

                val topArtists = parseTopArtists(topArtistsJson)
                val topTracks = fetchTrackMetadata(topTracksJson)

                _uiState.value = HomeUiState.Success(topArtists, topTracks)
                loadUsername()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadUsername() {
        try {
            _username.value = spClient.username()
        } catch (e: Exception) {
            // Ignore username errors
        }
    }

    private fun parseTopArtists(raw: String): List<TopArtist> {
        return try {
            val data = json.decodeFromString<TopArtistsResponse>(raw)
            data.items.map { artist ->
                TopArtist(
                    uri = artist.uri ?: "",
                    name = artist.name,
                    imageUrl = artist.images?.firstOrNull()?.url
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTrackMetadata(raw: String): List<Track> {
        return try {
            val data = json.decodeFromString<TopTracksResponse>(raw)
            val trackUris = data.items.mapNotNull { it.uri }.filter { it.startsWith("spotify:track:") }

            if (trackUris.isEmpty()) {
                return emptyList()
            }

            trackMetadataHelper.getTrackMetadata(trackUris)
        } catch (e: Exception) {
            emptyList()
        }
    }

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

    @Serializable
    private data class TopArtistsResponse(
        val items: List<TopArtistItem> = emptyList(),
    )

    @Serializable
    private data class TopArtistItem(
        val id: String? = null,
        val name: String = "",
        val uri: String? = null,
        val images: List<Image>? = null,
    )

    @Serializable
    private data class TopTracksResponse(
        val items: List<TopTrackItem> = emptyList(),
    )

    @Serializable
    private data class TopTrackItem(
        val id: String? = null,
        val name: String = "",
        val uri: String? = null,
        val duration_ms: Int? = null,
        val artists: List<Artist>? = null,
        val album: Album? = null,
    )

    @Serializable
    private data class Image(val url: String)

    @Serializable
    private data class Artist(val name: String)

    @Serializable
    private data class Album(val images: List<Image>? = null)
}