package cc.tomko.outify.ui.screens.search

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.R
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.getCover
import cc.tomko.outify.ui.components.navigation.Route.AlbumScreenFromTrackUri
import cc.tomko.outify.ui.components.navigation.Route.ArtistScreen
import cc.tomko.outify.ui.components.navigation.Route.PlaylistScreen
import cc.tomko.outify.ui.components.rows.ArtistRow
import cc.tomko.outify.ui.components.rows.PlaylistRow
import cc.tomko.outify.ui.components.rows.SwipeableTrackRow
import cc.tomko.outify.ui.viewmodel.SearchUiModel
import cc.tomko.outify.ui.viewmodel.SearchViewModel

@Composable
fun SharedTransitionScope.SearchScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val spirc = viewModel.spirc

    val listState = rememberLazyListState()
    val currentTrack by viewModel.currentTrack().collectAsState(initial = null)

    var showTracks by rememberSaveable { mutableStateOf(true) }
    var showArtists by rememberSaveable { mutableStateOf(true) }
    var showAlbums by rememberSaveable { mutableStateOf(true) }
    var showPlaylists by rememberSaveable { mutableStateOf(true) }
    var showShows by rememberSaveable { mutableStateOf(false) }
    var showEpisodes by rememberSaveable { mutableStateOf(false) }

    val filteredResults = remember(results, showTracks, showArtists, showAlbums, showPlaylists, showShows, showEpisodes) {
        applyFiltersToSectionedResults(
            results,
            showTracks,
            showArtists,
            showAlbums,
            showPlaylists,
            showShows,
            showEpisodes
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        MaterialSearchBar(
            onQueryChange = viewModel::onQueryChange,
            isLoading = isLoading
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FiltersBar(
                    showTracks = showTracks,
                    showArtists = showArtists,
                    showAlbums = showAlbums,
                    showPlaylists = showPlaylists,
                    showShows = showShows,
                    showEpisodes = showEpisodes,
                    onToggleTracks = { showTracks = it },
                    onToggleArtists = { showArtists = it },
                    onToggleAlbums = { showAlbums = it },
                    onTogglePlaylists = { showPlaylists = it },
                    onToggleShows = { showShows = it },
                    onToggleEpisodes = { showEpisodes = it }
                )
            }

            items(
                items = filteredResults,
                key = { it.uri }
            ) { item ->
                when (item) {
                    is SearchUiModel.SectionHeader -> {
                        Text(
                            text = stringResource(id = item.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                        )
                    }

                    is SearchUiModel.TrackItem -> {
                        val track = item.track
                        SwipeableTrackRow(
                            track = track,
                            currentTrack = currentTrack,
                            onRowClick = {
                                spirc.load(item.uri)
                            },
                            onArtworkClick = {
                                backStack.add(AlbumScreenFromTrackUri(item.uri))
                            },
                            favoriteTrack = { uri ->
                                viewModel.saveItem(uri)
                            }
                        )
                    }
                    is SearchUiModel.AlbumItem -> {

                    }
                    is SearchUiModel.ArtistItem -> {
                        val artist = item.artist

                        ArtistRow(
                            artist = artist,
                            artworkUrl = ALBUM_COVER_URL + artist.getCover(CoverSize.MEDIUM)?.uri,
                            onRowClick = {
                                backStack.add(ArtistScreen(artist.uri))
                            }
                        )
                    }
                    is SearchUiModel.PlaylistItem -> {
                        val playlist = item.playlist
                        var artworkUrl by remember(playlist.uri) { mutableStateOf<String?>(null) }

                        LaunchedEffect(playlist.uri) {
                            artworkUrl = viewModel.getArtworkUrl(playlist)
                        }

                        PlaylistRow(
                            playlist = playlist,
                            artworkUrl = artworkUrl,
                            onRowClick = {
                                backStack.add(PlaylistScreen(playlist.uri))
                            },
                            onArtistClick = {
                                // TODO: Add author page
                            },
                            contentDescription = playlist.attributes.description,
                        )
                    }
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

@Composable
fun FiltersBar(
    modifier: Modifier = Modifier,
    showTracks: Boolean,
    showArtists: Boolean,
    showAlbums: Boolean,
    showPlaylists: Boolean,
    showShows: Boolean,
    showEpisodes: Boolean,
    onToggleTracks: (Boolean) -> Unit,
    onToggleArtists: (Boolean) -> Unit,
    onToggleAlbums: (Boolean) -> Unit,
    onTogglePlaylists: (Boolean) -> Unit,
    onToggleShows: (Boolean) -> Unit,
    onToggleEpisodes: (Boolean) -> Unit,
    chipSpacing: Dp = 8.dp
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(chipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reusable builder for chips
        @Composable
        fun FilterChipWithTick(
            selected: Boolean,
            onClick: () -> Unit,
            label: @Composable () -> Unit
        ) {
            FilterChip(
                selected = selected,
                onClick = onClick,
                label = label,
                shape = RoundedCornerShape(20.dp),
                leadingIcon = {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    // unselected colors fall back to defaults
                )
            )
        }

        FilterChipWithTick(
            selected = showTracks,
            onClick = { onToggleTracks(!showTracks) }
        ) { Text(stringResource(R.string.search_section_tracks)) }

        FilterChipWithTick(
            selected = showArtists,
            onClick = { onToggleArtists(!showArtists) }
        ) { Text(stringResource(R.string.search_section_artists)) }

        FilterChipWithTick(
            selected = showAlbums,
            onClick = { onToggleAlbums(!showAlbums) }
        ) { Text(stringResource(R.string.search_section_albums)) }

        FilterChipWithTick(
            selected = showPlaylists,
            onClick = { onTogglePlaylists(!showPlaylists) }
        ) { Text(stringResource(R.string.search_section_playlists)) }

        // Optional: show & episodes if you support them
        FilterChipWithTick(
            selected = showShows,
            onClick = { onToggleShows(!showShows) }
        ) { Text(stringResource(R.string.search_section_shows)) }

        FilterChipWithTick(
            selected = showEpisodes,
            onClick = { onToggleEpisodes(!showEpisodes) }
        ) { Text(stringResource(R.string.search_section_episodes)) }
    }
}

private fun applyFiltersToSectionedResults(
    results: List<SearchUiModel>,
    showTracks: Boolean,
    showArtists: Boolean,
    showAlbums: Boolean,
    showPlaylists: Boolean,
    showShows: Boolean,
    showEpisodes: Boolean
): List<SearchUiModel> {
    val out = mutableListOf<SearchUiModel>()
    var i = 0
    while (i < results.size) {
        val item = results[i]
        if (item is SearchUiModel.SectionHeader) {
            // collect the section items
            val sectionItems = mutableListOf<SearchUiModel>()
            var j = i + 1
            while (j < results.size && results[j] !is SearchUiModel.SectionHeader) {
                sectionItems.add(results[j])
                j++
            }

            // filter the section items based on selected filters
            val filtered = sectionItems.filter { si ->
                when (si) {
                    is SearchUiModel.TrackItem -> showTracks
                    is SearchUiModel.ArtistItem -> showArtists
                    is SearchUiModel.AlbumItem -> showAlbums
                    is SearchUiModel.PlaylistItem -> showPlaylists
//                    is SearchUiModel.ShowItem -> showShows
//                    is SearchUiModel.EpisodeItem -> showEpisodes
                    else -> true
                }
            }

            if (filtered.isNotEmpty()) {
                out.add(item) // header
                out.addAll(filtered)
            }

            i = j // skip to next header or end
        } else {
            val include = when (item) {
                is SearchUiModel.TrackItem -> showTracks
                is SearchUiModel.ArtistItem -> showArtists
                is SearchUiModel.AlbumItem -> showAlbums
                is SearchUiModel.PlaylistItem -> showPlaylists
//                is SearchUiModel.ShowItem -> showShows
//                is SearchUiModel.EpisodeItem -> showEpisodes
            }
            if (include) out.add(item)
            i++
        }
    }
    return out
}