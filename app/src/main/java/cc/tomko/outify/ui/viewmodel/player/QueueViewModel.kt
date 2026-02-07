package cc.tomko.outify.ui.viewmodel.player

import android.app.Application
import androidx.lifecycle.ViewModel
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class QueueViewModel(
    val application: Application,
    val spirc: Spirc = OutifyApplication.spirc,
    val metadata: Metadata = (application as OutifyApplication).metadata,
    val json: Json = Json { ignoreUnknownKeys = true }
): ViewModel() {
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) {
            _queueState.value = QueueState(emptyList(), currentIndex = 0)
            return
        }

        val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
        _queueState.value = QueueState(tracks = tracks, currentIndex = safeIndex)
    }

    val currentTrack: Track?
        get() = _queueState.value.tracks.getOrNull(_queueState.value.currentIndex)

    suspend fun loadQueue(){
        val previousUris = json.decodeFromString<List<String>>(spirc.previousTracks())
        val previousTracks = metadata.getTrackMetadata(previousUris)

        val index = previousTracks.size + 1
        val currentTrack = OutifyApplication.playbackManager.playbackStateHolder.currentTrack.value

        val nextUris = json.decodeFromString<List<String>>(spirc.nextTracks())
        val nextTracks = metadata.getTrackMetadata(nextUris)

        val tracks = mutableListOf<Track>()
        tracks.addAll(previousTracks)
        if(currentTrack != null)
            tracks.add(currentTrack)
        tracks.addAll(nextTracks)

        setQueue(tracks, index)
    }

    // Move to next track
    fun nextTrack() {
        val state = _queueState.value
        val nextIndex = (state.currentIndex + 1).coerceAtMost(state.tracks.size - 1)
        _queueState.value = state.copy(currentIndex = nextIndex)
    }

    // Move to previous track
    fun prevTrack() {
        val state = _queueState.value
        val prevIndex = (state.currentIndex - 1).coerceAtLeast(0)
        _queueState.value = state.copy(currentIndex = prevIndex)
    }

    // Moves track
    fun moveTrack(oldIndex: Int, newIndex: Int) {
        val state = _queueState.value
        if (oldIndex !in state.tracks.indices || newIndex !in state.tracks.indices) return

        val mutable = state.tracks.toMutableList()
        val track = mutable.removeAt(oldIndex)
        mutable.add(newIndex, track)

        // Adjust currentIndex if needed
        val newCurrentIndex = when {
            oldIndex == state.currentIndex -> newIndex
            state.currentIndex in (oldIndex + 1)..newIndex -> state.currentIndex - 1
            state.currentIndex in newIndex..<oldIndex -> state.currentIndex + 1
            else -> state.currentIndex
        }

        _queueState.value = QueueState(tracks = mutable, currentIndex = newCurrentIndex)
    }

    // Remove a track
    fun removeTrack(index: Int) {
        val state = _queueState.value
        if (index !in state.tracks.indices) return

        val mutable = state.tracks.toMutableList()
        mutable.removeAt(index)

        val newCurrentIndex = when {
            index < state.currentIndex -> state.currentIndex - 1
            index == state.currentIndex -> state.currentIndex.coerceAtMost(mutable.lastIndex)
            else -> state.currentIndex
        }

        _queueState.value = QueueState(tracks = mutable, currentIndex = newCurrentIndex)
    }

    // Insert a track
    fun insertTrack(track: Track, index: Int) {
        val state = _queueState.value
        val mutable = state.tracks.toMutableList()
        val insertIndex = index.coerceIn(0, mutable.size)
        mutable.add(insertIndex, track)

        val newCurrentIndex = if (insertIndex <= state.currentIndex) state.currentIndex + 1 else state.currentIndex

        _queueState.value = QueueState(tracks = mutable, currentIndex = newCurrentIndex)
    }
}

data class QueueState(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = 0
)