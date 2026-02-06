package cc.tomko.outify.ui.viewmodel.library.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.toHeaderUi
import cc.tomko.outify.ui.screens.library.album.AlbumUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * View model for album screen
 */
class AlbumViewModel(
    application: Application,
    private val albumUri: String,
    private val metadata: Metadata = (application as OutifyApplication).metadata
): AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState

    fun loadAlbum() {
        println("Loading album")
        viewModelScope.launch {
            try {
                val album = metadata
                    .getAlbumMetadata(listOf(albumUri))
                    .first()

                val trackUris: List<String> = album.tracks.map { it }

                val tracks: List<Track> = if (trackUris.isNotEmpty()) {
                    metadata.getTrackMetadata(trackUris)
                } else emptyList()

                _uiState.value = AlbumUiState(
                    isLoading = false,
                    album = album.toHeaderUi(),
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

}