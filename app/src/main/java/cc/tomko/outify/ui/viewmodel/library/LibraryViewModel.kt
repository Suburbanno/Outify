package cc.tomko.outify.ui.viewmodel.library

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.metadata.Metadata
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val metadata: Metadata,
): ViewModel() {
    private val _headerArtwork = mutableStateOf<String?>(null)
    val headerArtwork = _headerArtwork

    private val playlistUris = MutableStateFlow<List<String>>(emptyList())

    val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val playlists: StateFlow<List<Playlist>> =
        playlistUris
            .flatMapLatest { uris ->
                if (uris.isEmpty()) {
                    flow { emit(emptyList()) }
                } else {
                    metadata.observePlaylists(uris)
                }
            }
            .debounce(50)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    suspend fun getArtworkUrl(playlist: Playlist): String? {
        return playlist.getCover(metadata)
    }

    fun loadPlaylistUris() {
        viewModelScope.launch {
            isRefreshing.value = true
            runCatching {
                metadata.getPlaylistUris()
            }.onSuccess { uris ->
                playlistUris.value = uris
            }.onFailure {
                Log.w("LibraryViewModel", "Failed to load playlist URIs", it)
            }
            isRefreshing.value = false
        }
    }

    fun loadHeaderArtwork(playlists: List<Playlist>) {
        if (_headerArtwork.value != null) return
        if (playlists.isNotEmpty()) {
            viewModelScope.launch {
                _headerArtwork.value =
                    getArtworkUrl(playlists.random())
            }
        }
    }

    fun refresh(){
        loadPlaylistUris()
    }
}