package cc.tomko.outify.ui.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Playlist
import cc.tomko.outify.data.Profile
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.profile.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val metadata: Metadata,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
    val userProfile: UserProfile,
): ViewModel() {
    val json = Json { ignoreUnknownKeys = true }

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    private val _authors = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val authors: StateFlow<Map<String, Profile>> = _authors

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState

    private val _trackMetadata = MutableStateFlow<Map<String, Track>>(emptyMap())
    val trackMetadata: StateFlow<Map<String, Track>> = _trackMetadata.asStateFlow()

    fun loadPlaylist(playlistUri: String) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading

            runCatching {
                metadata.getPlaylistMetadata(playlistUri)
            }.onSuccess { playlist ->
                _uiState.value = PlaylistUiState.Success(playlist)
            }.onFailure { e ->
                _uiState.value = PlaylistUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun trackFlow(uri: String): Flow<Track?> =
        trackMetadata
            .map { it[uri] }
            .distinctUntilChanged()

    suspend fun getOrLoadTrack(uri: String): Track? {
        trackMetadata.value[uri]?.let { return it }

        val fetched = withContext(Dispatchers.IO) {
            metadata.getTrackMetadata(listOf(uri)).firstOrNull()
        } ?: return null

        _trackMetadata.update { current -> current + (uri to fetched) }

        return fetched
    }

    fun loadMetadataIfNeeded(uris: List<String>) {
        val missing = uris.filterNot { _trackMetadata.value.containsKey(it) }
        if (missing.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val tracks = metadata.getTrackMetadata(missing)
            val results = tracks.associateBy { it.uri }
            _trackMetadata.update { current -> current + results }
        }
    }

    suspend fun getArtworkUrl(playlist: Playlist): String {
        if(playlist.attributes.pictureId.isNotEmpty()) {
            return ALBUM_COVER_URL + playlist.attributes.pictureId
        }

        // Getting first track
        val trackUri: String = playlist.contents.firstOrNull()?.uri ?: ""
        val track = metadata.getTrackMetadata(listOf(trackUri)).firstOrNull()

        return (ALBUM_COVER_URL + track?.album?.getCover(CoverSize.MEDIUM)?.uri)
    }

    suspend fun getAuthors(playlist: Playlist): List<Profile> = coroutineScope {
        val ids = playlist.contents
            .map { it.attributes.addedBy }
            .distinct()

        ids.map { id ->
            async(Dispatchers.IO) {
                val cached = _authors.value[id] ?: _authors.value[id]
                if (cached != null) return@async cached

                val jsonRaw = userProfile.getUserProfile(id)
                val profile = json.decodeFromString<Profile>(jsonRaw)

                _authors.update { current -> current + (profile.name!! to profile) }

                profile
            }
        }.awaitAll()
    }

    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }
}

sealed interface PlaylistUiState {
    object Loading : PlaylistUiState
    data class Success(val playlist: Playlist?) : PlaylistUiState
    data class Error(val error: String) : PlaylistUiState
}
