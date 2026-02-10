package cc.tomko.outify.ui.viewmodel.player

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

// ---- UI model: a unique queue entry (one id per occurrence) ----
data class QueueEntry(val id: Long, val track: Track)

class QueueViewModel(
    val application: Application,
    val spirc: Spirc = OutifyApplication.spirc,
    val metadata: Metadata = (application as OutifyApplication).metadata,
    val json: Json = Json { ignoreUnknownKeys = true }
) : ViewModel() {

    companion object {
        private const val INITIAL_LOAD_SIZE = 20 // Load 20 items initially
        private const val PAGE_SIZE = 15 // Load 15 items per page when scrolling
        private const val PREFETCH_THRESHOLD = 5 // Start loading when 5 items from edge

        // Simple atomic counter for stable per-occurrence IDs
        private val uidCounter = AtomicLong(0L)
        private fun nextId(): Long = uidCounter.incrementAndGet()
    }

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    // Cache for all track URIs (lightweight)
    private var allPreviousUris: List<String> = emptyList()
    private var allNextUris: List<String> = emptyList()

    // Loading jobs tracking
    private var previousLoadJob: Job? = null
    private var nextLoadJob: Job? = null

    val currentTrackEntry: QueueEntry?
        get() = _queueState.value.tracks.getOrNull(_queueState.value.currentIndex)

    /**
     * Initial queue load - fetches URIs and loads initial chunk
     */
    suspend fun loadQueue(currentTrack: Track?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _queueState.update { it.copy(isLoading = true, error = null) }

                allPreviousUris = loadPreviousUris()
                allNextUris = loadNextUris()

                val currentIndex = allPreviousUris.size

                val total = allPreviousUris.size + (if (currentTrack != null) 1 else 0) + allNextUris.size

                val half = INITIAL_LOAD_SIZE / 2
                val startIndex = max(0, currentIndex - half)
                val endIndex = min(total, currentIndex + half + 1) // end is exclusive

                val initialEntries = loadTracksInRange(startIndex, endIndex, currentTrack)

                _queueState.update {
                    it.copy(
                        tracks = initialEntries,
                        currentIndex = (initialEntries.indexOfFirst { entry ->
                            currentTrack != null && entry.track.uri == currentTrack.uri
                        }).let { idx -> if (idx >= 0) idx else (initialEntries.indexOfFirst { false }).coerceAtLeast(0) },
                        totalSize = total,
                        loadedRange = startIndex until endIndex,
                        isLoading = false,
                        isLoadingPrevious = false,
                        isLoadingNext = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _queueState.update { it.copy(isLoading = false, error = e.message) }
            }
        }.join()
    }

    /**
     * Load more tracks when user scrolls near the edges
     */
    fun onScrollPositionChanged(firstVisibleIndex: Int, lastVisibleIndex: Int, currentTrack: Track?) {
        val state = _queueState.value
        if (state.isLoading || state.tracks.isEmpty()) return
        if (state.isLoadingPrevious || state.isLoadingNext) return

        val loadedRange = state.loadedRange
        val canonicalFirst = loadedRange.first + firstVisibleIndex
        val canonicalLast = loadedRange.first + lastVisibleIndex

        // load previous pages
        if (canonicalFirst < loadedRange.first + PREFETCH_THRESHOLD && loadedRange.first > 0) {
            loadPreviousPage(currentTrack)
        } else if (canonicalLast > loadedRange.last - PREFETCH_THRESHOLD && loadedRange.last < state.totalSize) {
            loadNextPage(currentTrack)
        }
    }

    /**
     * Load previous page of tracks
     */
    private fun loadPreviousPage(currentTrack: Track?) {
        if (previousLoadJob?.isActive == true) return

        previousLoadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _queueState.value
                val currentRange = state.loadedRange
                val newStartIndex = max(0, currentRange.first - PAGE_SIZE)
                if (newStartIndex >= currentRange.first) return@launch

                _queueState.update { it.copy(isLoadingPrevious = true, error = null) }

                val newEntries = loadTracksInRange(newStartIndex, currentRange.first, currentTrack)

                _queueState.update { current ->
                    val updatedList = (newEntries + current.tracks)
                    // keep currentIndex offset by number of inserted entries at front
                    val newCurrentIndex = current.currentIndex + newEntries.size
                    current.copy(
                        tracks = updatedList,
                        currentIndex = newCurrentIndex,
                        loadedRange = newStartIndex until current.loadedRange.last,
                        isLoadingPrevious = false
                    )
                }
            } catch (e: Exception) {
                _queueState.update { it.copy(isLoadingPrevious = false, error = e.message) }
            }
        }
    }

    /**
     * Load next page of tracks
     */
    private fun loadNextPage(currentTrack: Track?) {
        if (nextLoadJob?.isActive == true) return

        nextLoadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _queueState.value
                val currentRange = state.loadedRange
                val newEndIndex = min(state.totalSize, currentRange.last + PAGE_SIZE)
                if (newEndIndex <= currentRange.last) return@launch

                _queueState.update { it.copy(isLoadingNext = true, error = null) }

                val newEntries = loadTracksInRange(currentRange.last, newEndIndex, currentTrack)

                _queueState.update { current ->
                    val updatedList = current.tracks + newEntries
                    current.copy(
                        tracks = updatedList,
                        loadedRange = current.loadedRange.first until newEndIndex,
                        isLoadingNext = false
                    )
                }
            } catch (e: Exception) {
                _queueState.update { it.copy(isLoadingNext = false, error = e.message) }
            }
        }
    }

    /**
     * Load tracks in a specific canonical index range
     */
    private suspend fun loadTracksInRange(
        startIndex: Int,
        endIndex: Int,
        currentTrack: Track?
    ): List<QueueEntry> = withContext(Dispatchers.IO) {
        val urisToLoad = mutableListOf<String>()
        val positionsToUriIndex = mutableListOf<Pair<Int, Int>>() // -1 for current
        val currentTrackIndex = allPreviousUris.size

        for (i in startIndex until endIndex) {
            when {
                i < allPreviousUris.size -> {
                    positionsToUriIndex += (i to urisToLoad.size)
                    urisToLoad.add(allPreviousUris[i])
                }
                i == currentTrackIndex && currentTrack != null -> {
                    // mark as current (no uri to load)
                    positionsToUriIndex += (i to -1)
                }
                i > currentTrackIndex -> {
                    val nextIndex = i - currentTrackIndex - 1
                    if (nextIndex < allNextUris.size) {
                        positionsToUriIndex += (i to urisToLoad.size)
                        urisToLoad.add(allNextUris[nextIndex])
                    } else {
                        // out of range; ignore
                        positionsToUriIndex += (i to -2)
                    }
                }
            }
        }

        // Fetch metadata for pages (we already collected URIsToLoad)
        val loadedTracks: List<Track> = if (urisToLoad.isNotEmpty()) {
            try {
                metadata.getTrackMetadata(urisToLoad)
            } catch (t: Throwable) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Now map canonical positions -> QueueEntry
        val entries = mutableListOf<QueueEntry>()
        var loadedCursor = 0
        for ((canonicalIndex, uriLoadIndex) in positionsToUriIndex) {
            when {
                uriLoadIndex >= 0 -> {
                    // take next loaded track (mapping relies on same ordering)
                    val track = loadedTracks.getOrNull(loadedCursor)
                    if (track != null) {
                        entries.add(QueueEntry(nextId(), track))
                        loadedCursor++
                    }
                }
                uriLoadIndex == -1 -> {
                    // current track
                    if (currentTrack != null) {
                        entries.add(QueueEntry(nextId(), currentTrack))
                    }
                }
                else -> {
                    // placeholder or out-of-range; skip or add nothing
                }
            }
        }

        entries
    }

    /**
     * Set queue manually (for local operations) from a list of QueueEntry
     */
    fun setQueueEntries(entries: List<QueueEntry>, startIndex: Int = 0) {
        if (entries.isEmpty()) {
            _queueState.value = QueueState(emptyList(), currentIndex = 0)
            return
        }
        val safeIndex = startIndex.coerceIn(0, entries.size - 1)
        _queueState.value = QueueState(
            tracks = entries,
            currentIndex = safeIndex,
            totalSize = entries.size,
            loadedRange = 0 until entries.size
        )
    }

    /**
     * Convenience: set queue from simple Track list (generates fresh QueueEntry ids)
     */
    fun setQueueFromTracks(tracks: List<Track>, startIndex: Int = 0) {
        val entries = tracks.map { QueueEntry(nextId(), it) }
        setQueueEntries(entries, startIndex)
    }

    // Move to next track
    fun nextTrack() {
        val state = _queueState.value
        val nextIndex = (state.currentIndex + 1).coerceAtMost(state.tracks.size - 1)
        _queueState.update { it.copy(currentIndex = nextIndex) }
    }

    // Move to previous track
    fun prevTrack() {
        val state = _queueState.value
        val prevIndex = (state.currentIndex - 1).coerceAtLeast(0)
        _queueState.update { it.copy(currentIndex = prevIndex) }
    }

    // Move track (indices are in entries space)
    fun moveTrack(from: Int, to: Int) {
        if (from == to) return

        viewModelScope.launch(Dispatchers.Default) {
            val list = _queueState.value.tracks.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)

            val current = _queueState.value.currentIndex
            val newCurrentIndex = when {
                from == current -> to
                from < current && to >= current -> current - 1
                from > current && to <= current -> current + 1
                else -> current
            }

            _queueState.update {
                it.copy(
                    tracks = list,
                    currentIndex = newCurrentIndex
                )
            }
        }
    }

    // Remove a track (by entry index)
    fun removeTrack(index: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _queueState.value
            if (index !in state.tracks.indices) return@launch

            val mutable = state.tracks.toMutableList()
            mutable.removeAt(index)

            val newCurrentIndex = when {
                index < state.currentIndex -> state.currentIndex - 1
                index == state.currentIndex -> state.currentIndex.coerceAtMost(mutable.lastIndex)
                else -> state.currentIndex
            }

            _queueState.update {
                QueueState(
                    tracks = mutable,
                    currentIndex = newCurrentIndex,
                    totalSize = mutable.size,
                    loadedRange = 0 until mutable.size
                )
            }
        }
    }

    // Insert a track (creates an entry)
    fun insertTrack(track: Track, index: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _queueState.value
            val mutable = state.tracks.toMutableList()
            val insertIndex = index.coerceIn(0, mutable.size)
            mutable.add(insertIndex, QueueEntry(nextId(), track))

            val newCurrentIndex = if (insertIndex <= state.currentIndex) {
                state.currentIndex + 1
            } else {
                state.currentIndex
            }

            _queueState.update {
                QueueState(
                    tracks = mutable,
                    currentIndex = newCurrentIndex,
                    totalSize = mutable.size,
                    loadedRange = 0 until mutable.size
                )
            }
        }
    }

    // Helpers

    private suspend fun loadPreviousUris(): List<String> = withContext(Dispatchers.IO) {
        try {
            val previousUrisRaw = spirc.previousTracks()
            json.decodeFromString<List<String>>(previousUrisRaw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun loadNextUris(): List<String> = withContext(Dispatchers.IO) {
        try {
            val nextUrisRaw = spirc.nextTracks()
            json.decodeFromString<List<String>>(nextUrisRaw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        previousLoadJob?.cancel()
        nextLoadJob?.cancel()
    }
}

data class QueueState(
    val tracks: List<QueueEntry> = emptyList(),
    val currentIndex: Int = 0,
    val totalSize: Int = 0,
    val loadedRange: IntRange = 0 until 0,
    val isLoading: Boolean = false,
    val isLoadingPrevious: Boolean = false,
    val isLoadingNext: Boolean = false,
    val error: String? = null
)