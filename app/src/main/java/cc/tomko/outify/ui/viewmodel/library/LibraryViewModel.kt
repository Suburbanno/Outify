package cc.tomko.outify.ui.viewmodel.library

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.UserProfile
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.Profile
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.metadata.Metadata
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.collections.plus

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val metadata: Metadata,
    private val json: Json,
    private val userProfile: UserProfile,
): ViewModel() {

    init {
        viewModelScope.launch {
            metadata.syncLikedPlaylists()
        }
    }
    private val _headerArtwork = mutableStateOf<String?>(null)
    val headerArtwork = _headerArtwork

    private val playlistUris = MutableStateFlow<List<String>>(emptyList())

    private val _authors = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val authors: StateFlow<Map<String, Profile>> = _authors
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

    suspend fun getAuthors(playlist: Playlist): List<Profile> = coroutineScope {
        val ids = playlist.contents
            .map { it.attributes.addedBy }
            .distinct()

        ids.map { id ->
            async(Dispatchers.IO) {
                _authors.value[id]?.let { return@async it }

                val jsonRaw = try {
                    userProfile.getUserProfile(id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: return@async null

                val profile = try {
                    json.decodeFromString<Profile>(jsonRaw)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@async null
                }

                _authors.update { current -> current + (id to profile) }

                profile
            }
        }.awaitAll()
            .filterNotNull()
    }

    fun refresh(){
        loadPlaylistUris()
    }
}