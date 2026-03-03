package cc.tomko.outify.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.R
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Playlist
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.model.search.SearchResultType
import cc.tomko.outify.ui.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val metadata: Metadata,
    val spirc: SpircWrapper,
    val spClient: SpClient,
    private val repository: SearchRepository,
    private val playbackStateHolder: PlaybackStateHolder,
): ViewModel() {
    private val queryFlow = MutableStateFlow("")

    private val _results = MutableStateFlow<List<SearchUiModel>>(emptyList())
    val results: StateFlow<List<SearchUiModel>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun currentTrack(): Flow<Track?> =
        playbackStateHolder.state.map { it.currentTrack }

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->

                    if (query.isBlank()) {
                        _results.value = emptyList()
                        return@collectLatest
                    }

                    _isLoading.value = true

                    try {
                        val searchResults = repository.search(query)

                        val grouped = searchResults.groupBy { it.type }
                        val sectionedList = mutableListOf<SearchUiModel>()

                        fun <T> appendSectionIfPresent(
                            type: SearchResultType,
                            titleRes: Int,
                            map: Map<String, T>,
                            mapper: (String, T) -> SearchUiModel
                        ) {
                            val items = grouped[type] ?: return
                            if (items.isEmpty()) return

                            val mapped = items.mapNotNull { result ->
                                map[result.uri]?.let { data ->
                                    mapper(result.uri, data)
                                }
                            }

                            if (mapped.isNotEmpty()) {
                                sectionedList.add(SearchUiModel.SectionHeader(titleRes))
                                sectionedList.addAll(mapped)
                            }
                        }

                        coroutineScope {
                            val trackDeferred = async(Dispatchers.IO) {
                                grouped[SearchResultType.TRACK]
                                    ?.map { it.uri }
                                    ?.let { metadata.getTrackMetadata(it).associateBy { t -> t.uri } }
                                    ?: emptyMap()
                            }

                            val artistDeferred = async(Dispatchers.IO) {
                                searchResults
                                    .filter { it.type == SearchResultType.ARTIST }
                                    .mapNotNull { result ->
                                        runCatching {
                                            metadata.getArtistMetadata(result.uri)
                                        }.getOrNull()?.let {
                                            result.uri to it
                                        }
                                    }.toMap()
                            }

                            val albumDeferred = async(Dispatchers.IO) {
                                searchResults
                                    .filter { it.type == SearchResultType.ALBUM }
                                    .mapNotNull { result ->
                                        runCatching {
                                            metadata.getAlbumMetadata(result.uri)
                                        }.getOrNull()?.let {
                                            result.uri to it
                                        }
                                    }.toMap()
                            }

                            val playlistDefered = async(Dispatchers.IO) {
                                searchResults
                                    .filter { it.type == SearchResultType.PLAYLIST }
                                    .mapNotNull { result ->
                                        runCatching {
                                            metadata.getPlaylistMetadata(result.uri)
                                        }.getOrNull()?.let {
                                            result.uri to it
                                        }
                                    }.toMap()
                            }

                            val trackMap: Map<String, Track> = trackDeferred.await()
                            appendSectionIfPresent(
                                type = SearchResultType.TRACK,
                                titleRes = R.string.search_section_tracks,
                                map = trackMap
                            ) { uri, track -> SearchUiModel.TrackItem(uri, track) }
                            _results.value = sectionedList.toList()

                            val artistMap: Map<String, Artist> = artistDeferred.await()
                            appendSectionIfPresent(
                                type = SearchResultType.ARTIST,
                                titleRes = R.string.search_section_artists,
                                map = artistMap
                            ) { uri, artist -> SearchUiModel.ArtistItem(uri, artist) }
                            _results.value = sectionedList.toList()

                            val albumMap: Map<String, Album> = albumDeferred.await()
                            appendSectionIfPresent(
                                type = SearchResultType.ALBUM,
                                titleRes = R.string.search_section_albums,
                                map = albumMap
                            ) { uri, album -> SearchUiModel.AlbumItem(uri, album) }
                            _results.value = sectionedList.toList()

                            val playlistMap: Map<String, Playlist> = playlistDefered.await()
                            appendSectionIfPresent(
                                type = SearchResultType.PLAYLIST,
                                titleRes = R.string.search_section_playlists,
                                map = playlistMap
                            ) { uri, playlist -> SearchUiModel.PlaylistItem(uri, playlist) }
                            _results.value = sectionedList.toList()
                        }

                    } catch (e: Exception) {
                        _results.value = emptyList()
                    } finally {
                        _isLoading.value = false
                    }
                }
        }
    }

    fun onQueryChange(query: String){
        queryFlow.value = query
    }

    suspend fun getArtworkUrl(playlist: Playlist): String {
        if(playlist.attributes.pictureId.isNotEmpty()) {
            return ALBUM_COVER_URL + playlist.attributes.pictureId
        }

        // Getting first track
        val trackUri: String = playlist.contents.firstOrNull()?.uri ?: ""
        val track = metadata.getTrackMetadata(listOf(trackUri)).firstOrNull()

        return (ALBUM_COVER_URL + track?.album?.getCover(CoverSize.MEDIUM)?.uri)
    }

    fun saveItem(uri: String) {
        viewModelScope.launch {
            if(!spClient.saveItems(arrayOf(uri))){
                Log.w("SearchViewModel", "saveItem failed", )
            }
        }
    }

    fun startRadio(uri: String) {
        viewModelScope.launch {
            spirc.startRadio(uri, false)
        }
    }

    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }
}

sealed class SearchUiModel {
    abstract val uri: String

    data class SectionHeader(
        @StringRes val titleRes: Int
    ) : SearchUiModel() {
        override val uri: String = "header_$titleRes"
    }

    data class TrackItem(
        override val uri: String,
        val track: Track
    ) : SearchUiModel()

    data class ArtistItem(
        override val uri: String,
        val artist: Artist
    ) : SearchUiModel()

    data class AlbumItem(
        override val uri: String,
        val album: Album
    ) : SearchUiModel()

    data class PlaylistItem(
        override val uri: String,
        val playlist: Playlist
    ) : SearchUiModel()

//    data class ShowItem(
//        override val uri: String,
//        val show: Show
//    ) : SearchUiModel()
//
//    data class EpisodeItem(
//        override val uri: String,
//        val episode: Episode
//    ) : SearchUiModel()
}