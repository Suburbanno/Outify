package cc.tomko.outify.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.query
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.model.search.SearchResult
import cc.tomko.outify.ui.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    metadata: Metadata,
    val spirc: SpircWrapper,
    private val repository: SearchRepository,
    private val playbackStateHolder: PlaybackStateHolder,
): ViewModel() {
    private val queryFlow = MutableStateFlow("")

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _trackMap = MutableStateFlow<Map<String, Track>>(emptyMap())
    val trackMap: StateFlow<Map<String, Track>> = _trackMap

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isEmpty()) {
                        _results.value = emptyList()
                        _trackMap.value = emptyMap()
                        return@collectLatest
                    }

                    _isLoading.value = true
                    try {
                        val res = repository.search(query)
                        _results.value = res

                        val trackUris = res.filter { it.uri.startsWith("spotify:track:") }.map { it.uri }
                        if (trackUris.isNotEmpty()) {
                            val tracks = try {
                                metadata.getTrackMetadata(trackUris)
                            } catch (e: Exception) {
                                emptyList()
                            }
                            _trackMap.value = tracks.associateBy { it.uri }
                        } else {
                            _trackMap.value = emptyMap()
                        }

                    } finally {
                        _isLoading.value = false
                    }
                }
        }
    }

    fun onQueryChange(query: String){
        queryFlow.value = query
    }
}