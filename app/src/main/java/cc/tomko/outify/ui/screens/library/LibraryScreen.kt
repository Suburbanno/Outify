package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.PlaylistRow
import cc.tomko.outify.ui.viewmodel.library.LibraryUiState
import cc.tomko.outify.ui.viewmodel.library.LibraryViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.LibraryScreen(
    viewModel: LibraryViewModel,
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val playlists by viewModel.playlists.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPlaylistUris() }

    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current

    when (uiState) {
        is LibraryUiState.Loading -> {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
                contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
        }
        is LibraryUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (uiState as LibraryUiState.Error).error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is LibraryUiState.Success -> {
            val lazyListState = rememberLazyListState()
            val collapsingState = rememberCollapsingHeaderState()

            val isRefreshing by viewModel.isRefreshing.collectAsState()

            LaunchedEffect(lazyListState.isScrollInProgress) {
                if (!lazyListState.isScrollInProgress) {
                    val canExpand =
                        lazyListState.firstVisibleItemIndex == 0 &&
                                lazyListState.firstVisibleItemScrollOffset == 0

                    collapsingState.snapIfNeeded(canExpand)
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface)
                    .nestedScroll(collapsingState.nestedScrollConnection)
            ) {
                val currentTopBarHeightDp =
                    with(density) { collapsingState.height.value.toDp() }

                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(
                        top = currentTopBarHeightDp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = playlists,
                        key = { it.uri },
                        contentType = { "playlist" }
                    ) { playlist ->
                        val artworkUrl by produceState<String?>(
                            initialValue = null,
                            key1 = playlist.uri
                        ) {
                            value = viewModel.getArtworkUrl(playlist)
                        }

                        PlaylistRow(
                            playlist = playlist,
                            artworkUrl = artworkUrl,
                            onRowClick = {
                                backStack.add(Route.PlaylistScreen(playlist.uri))
                            }
                        )
                    }
                }

                CollapsingHeader(
                    collapseFraction = collapsingState.collapseFraction,
                    headerHeight = currentTopBarHeightDp,
                    onBackPressed = {
                        backStack.removeAt(backStack.lastIndex)
                    },
                    backgroundContent = {
                        var artworkUrl by viewModel.headerArtwork
                        LaunchedEffect(playlists) {
                            viewModel.loadHeaderArtwork(playlists)
                        }

                        ArtworkBackground(
                            artworkUrl = artworkUrl ?: "", // TODO: Some placeholder when null
                        )
                    },
                    titleContent = {
                        Text(
                            text = "Your library",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "• ${playlists.count()} playlists",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                )
            }
        }
    }
}