package cc.tomko.outify.ui.viewmodel.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.core.model.toOutifyUri
import cc.tomko.outify.data.dao.PlaylistDao
import cc.tomko.outify.data.database.playlist.canModify
import cc.tomko.outify.data.database.playlist.toDomain
import cc.tomko.outify.data.metadata.Metadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val spClient: SpClient,
    private val metadata: Metadata,
) : ViewModel() {
    private val usernameFlow = flow {
        emit(spClient.username())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    private val playlistsWithArtwork = playlistDao.getPlaylistsWithItemsFlow()
        .combine(usernameFlow) { playlists, username ->
            playlists
                .filter { it.canModify(username) }
                .map { entity ->
                    val playlist = entity.toDomain()
                    val artwork = playlist.getCover(metadata, CoverSize.MEDIUM)
                    PlaylistUi(playlist = playlist, artworkUrl = artwork)
                }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val ownedPlaylists = playlistsWithArtwork

    fun addToPlaylist(track: Track, playlist: Playlist) {
        // TODO: Add optimistic UI
        viewModelScope.launch {
            spClient.addToPlaylist(playlist.id, arrayOf(track.uri))
        }
    }

    data class PlaylistUi(
        val playlist: Playlist,
        val artworkUrl: String?
    )
}