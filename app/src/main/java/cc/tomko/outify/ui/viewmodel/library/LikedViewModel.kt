package cc.tomko.outify.ui.viewmodel.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedViewModel @Inject constructor(
    private val repo: LibraryRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper
) : ViewModel() {
    private val _likedTracks = MutableStateFlow<List<Track>>(emptyList())
    val likedTracks: StateFlow<List<Track>> = _likedTracks

    private var offset = 0
    private val pageSize = 50
    private var isLoading = false
    private var endReached = false

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    fun ensureLoaded(){
        if(_likedTracks.value.isEmpty() && !isLoading && !endReached) {
            loadNextPage()
        }
    }

    /**
     * Load the next page of liked tracks from the repository.
     * Safe to call multiple times; will coalesce using isLoading/endReached flags.
     */
    fun loadNextPage() {
        if (isLoading || endReached) return

        isLoading = true
        viewModelScope.launch {
            try {
                val page = repo.getLikedTracks(limit = pageSize, offset = offset)
                if (page.isEmpty()) {
                    endReached = true
                } else {
                    offset += page.size
                    _likedTracks.value = _likedTracks.value + page
                }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Called by the UI when the current visible end index approaches the end of the list.
     * Triggers loading the next page if needed.
     */
    fun onVisibleIndex(visibleIndex: Int) {
        val threshold = 10
        val size = _likedTracks.value.size
        if (!endReached && visibleIndex + threshold >= size) {
            loadNextPage()
        }
    }

    fun refresh() {
        offset = 0
        endReached = false
        _likedTracks.value = emptyList()
        loadNextPage()
    }
}