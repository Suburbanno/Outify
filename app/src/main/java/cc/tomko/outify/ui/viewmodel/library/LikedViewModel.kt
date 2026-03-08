package cc.tomko.outify.ui.viewmodel.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.toDomain
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.repository.LikedRepository
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class LikedViewModel @Inject constructor(
    val spirc: SpircWrapper,
    val imageLoader: ImageLoader,
    private val likedRepository: LikedRepository,
    private val playbackStateHolder: PlaybackStateHolder,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)

    companion object {
        private const val PAGE_SIZE = 30
        private const val PREFETCH_THRESHOLD = 8
    }

    /** Total liked count from liked_songs — available even before metadata loads */
    val totalCount: StateFlow<Int> = likedRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedTracks: StateFlow<List<Track>> = likedRepository.observeLikedTracksWithDetails()
        .mapLatest { rows ->
            if (rows.isEmpty()) return@mapLatest emptyList()

            val albums = likedRepository.getAlbumsForTracks(rows)

            rows.mapNotNull { twa ->
                runCatching {
                    twa.toDomain(twa.track.albumId?.let { albums[it] })
                }.getOrNull()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Guards against duplicate concurrent fetches
    private val fetchLock = Mutex()
    private var lastFetchedOffset = -1

    init {
        refresh()
    }

    fun refresh() {
        if(spirc.isUsable) {
            viewModelScope.launch {
                isRefreshing.value = true
                likedRepository.syncLikedUris()
                isRefreshing.value = false
            }
            // Kick off the first page
            triggerLoad(offset = 0)
        }
    }

    /**
     * Called from the screen as the visible index advances.
     */
    fun onVisibleIndex(visibleIndex: Int) {
        val loaded = likedTracks.value.size
        if (visibleIndex >= loaded - PREFETCH_THRESHOLD) {
            triggerLoad(offset = loaded)
        }
    }

    private fun triggerLoad(offset: Int) {
        if (offset == lastFetchedOffset) return
        viewModelScope.launch {
            fetchLock.withLock {
                if (offset == lastFetchedOffset) return@withLock
                lastFetchedOffset = offset
                runCatching {
                    likedRepository.ensureWindowLoaded(offset, PAGE_SIZE)
                }.onFailure {
                    Log.w("LikedViewModel", "Failed to load window at $offset", it)
                }
            }
        }
    }

    fun getArtwork(): Flow<String?> =
        likedTracks.map { tracks ->
            tracks.firstOrNull()
                ?.album
                ?.getCover(CoverSize.LARGE)
                ?.uri
        }

    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun isPlaying(): Flow<Boolean> =
        playbackStateHolder.state.map { it.isPlaying }
}