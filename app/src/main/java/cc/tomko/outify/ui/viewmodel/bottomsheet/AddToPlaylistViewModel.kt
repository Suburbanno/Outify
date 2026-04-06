package cc.tomko.outify.ui.viewmodel.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.data.dao.PlaylistDao
import cc.tomko.outify.data.database.playlist.canModify
import cc.tomko.outify.data.database.playlist.toDomain
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
                    PlaylistUi(playlist = playlist, artworkUrl = entity.playlist.cachedArtworkUrl)
                }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val ownedPlaylists = playlistsWithArtwork

    init {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.getPlaylistsWithItemsFlow().collect { playlists ->
                playlists.forEach { pwi ->
                    val playlist = pwi.toDomain()
                    if (pwi.playlist.cachedArtworkUrl.isNullOrEmpty() && playlist.attributes.pictureId.isEmpty()) {
                        val trackId = playlist.contents.firstOrNull()?.id
                        if (trackId != null) {
                            try {
                                val coverUris = playlistDao.getCoverUris(trackId)
                                val artworkUrl = coverUris?.let { "${it.baseUrl}${it.medium ?: it.small ?: it.large ?: ""}" }
                                playlistDao.updateCachedArtworkUrl(playlist.id, artworkUrl)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    data class PlaylistUi(
        val playlist: Playlist,
        val artworkUrl: String?
    )
}