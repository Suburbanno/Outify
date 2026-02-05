package cc.tomko.outify.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.components.TrackRow
import cc.tomko.outify.ui.components.TrackRowDensity
import cc.tomko.outify.ui.model.search.SearchResultType
import cc.tomko.outify.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val trackMap by viewModel.trackMap.collectAsState()

    val listState = rememberLazyListState()
    val currentTrack by OutifyApplication.playbackManager.playbackStateHolder.currentTrack.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        MaterialSearchBar(
            onQueryChange = viewModel::onQueryChange,
            isLoading = isLoading
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialSearchBar(
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val expanded = false

    // Auto focusing the searchbar
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if(autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                query = query,
                onQueryChange = { new ->
                    query = new
                    onQueryChange(new)
                },
                onSearch = {
                    onQueryChange(query)
                    keyboardController?.hide()
                },
                expanded = expanded,
                onExpandedChange = { /* no-op: keep collapsed so results render below */ },
                placeholder = { Text(text = "Search Spotify") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (isLoading) {
                        ContainedLoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        // clear button
                        IconButton(onClick = {
                            query = ""
                            onQueryChange("")
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear query")
                        }
                    }
                }
            )
        },
        expanded = expanded,
        onExpandedChange = { /* ignored */ }
    ) {}
}