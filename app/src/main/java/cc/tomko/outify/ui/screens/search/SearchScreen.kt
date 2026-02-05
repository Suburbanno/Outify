package cc.tomko.outify.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.components.TrackRowDensity
import cc.tomko.outify.ui.model.search.SearchResultType
import cc.tomko.outify.ui.viewmodel.SearchViewModel
import coil3.ImageLoader

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val trackMap by viewModel.trackMap.collectAsState()

    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val currentTrack by OutifyApplication.playbackManager.playbackStateHolder.currentTrack.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        MaterialSearchBar(
            onQueryChange = viewModel::onQueryChange,
            isLoading = isLoading
        )

        LazyColumn(state = listState) {
            items(
                items = results,
                key = { it.uri }
            ) { item ->
                when (item.resultType) {
                    SearchResultType.TRACK -> {
                        val domainTrack = trackMap[item.uri]
                        if (domainTrack != null) {
                            TrackRow(
                                title = domainTrack.name,
                                artist = domainTrack.artists.joinToString(", ") { it.name },
                                artworkUrl = OutifyApplication.ALBUM_COVER_URL + domainTrack.album?.covers?.firstOrNull()?.uri,
                                isPlaying = currentTrack?.uri.equals(item.uri),
                                isSelected = false,
                                density = TrackRowDensity.Default,
                                onRowClick = {
                                    OutifyApplication.spirc.load(item.uri)
                                }
                            )
                        } else {
                            // TODO: Add placeholder as we wait for metadata
                        }
                    }
                    SearchResultType.ARTIST -> TODO("Show artist row")
                    SearchResultType.EPISODE -> TODO("Show episode row")
                    SearchResultType.PLAYLIST -> TODO("Show playlist row")
                    SearchResultType.RADIO -> TODO("Show radio row")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MaterialSearchBar(
    onQueryChange: (String) -> Unit,
    isLoading: Boolean
) {
    var query by rememberSaveable { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchBar(
            query = query,
            onQueryChange = { new ->
                query = new
                onQueryChange(new)
            },
            onSearch = { finalQuery ->
                onQueryChange(finalQuery)
                active = false
            },
            active = active,
            onActiveChange = { active = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon"
                )
            },
            trailingIcon = {
                if (isLoading) {
                    ContainedLoadingIndicator(modifier = Modifier.size(20.dp))
                } else {
                    // optional: a clear/search action
                    IconButton(onClick = { query = ""; onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Clear or search"
                        )
                    }
                }
            },
            placeholder = { Text("Search Spotify") }
        ) {
            // Suggestion content goes here when `active` is true.
            // For now we keep this empty — you can display recent searches / suggestions.
        }
    }
}