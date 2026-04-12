package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.PlaylistRow
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

    val density = LocalDensity.current
    val sharedTransitionScope = this

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
        onRefresh = { viewModel.refresh() },
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
            .nestedScroll(collapsingState.nestedScrollConnection)
    ) {
        val currentTopBarHeightDp = with(density) { collapsingState.height.value.toDp() }

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = currentTopBarHeightDp),
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
                    },
                    sharedTransitionScope = this@LibraryScreen
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
                    artworkUrl = artworkUrl,
                    fallback = {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = null
                        )
                    }
                )
            },
            titleContent = {
                Text(
                    text = "Your library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Account • ${playlists.count()} playlists",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
        )
    }
}