package cc.tomko.outify.ui.viewmodel.library.album

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.dao.LikedDao
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.screens.library.album.AlbumUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
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
    val likedDao: LikedDao,
): ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedTrackIds: StateFlow<Set<String>> =
        likedDao.observeLikedIds()
            .map { it.toHashSet() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet()
            )

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun isPlaying(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isPlaying }

    suspend fun loadAlbum(albumUri: String) {
        try {
            val album = withContext(Dispatchers.IO) {
                metadata.getAlbumMetadata(albumUri)
            }

            if (album == null) {
                _uiState.value = AlbumUiState(
                    isLoading = false,
                    error = "Album not found"
                )
                return
            }

            val trackUris: List<String> = album.tracks.map { it }

            val tracks: List<Track> = if (trackUris.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    metadata.getTrackMetadata(trackUris)
                }
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

    suspend fun loadAlbumFromTrackUri(trackUri: String) {
        try {
            val albumId = withContext(Dispatchers.IO) {
                metadata.getTrackAlbumId(trackUri)
            }

            if (albumId == null) {
                _uiState.value = AlbumUiState(
                    isLoading = false,
                    error = "Album for track not found"
                )
                return
            }

            loadAlbum("spotify:album:$albumId")
        } catch (e: Exception) {
            _uiState.value = AlbumUiState(
                isLoading = false,
                error = e.message
            )
        }
    }

    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }
}