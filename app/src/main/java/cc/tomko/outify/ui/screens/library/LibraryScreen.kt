package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.core.model.Profile
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.PopupSpec
import cc.tomko.outify.ui.components.ArtworkBackground
import cc.tomko.outify.ui.components.CollapsingHeader
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.rememberCollapsingHeaderState
import cc.tomko.outify.ui.components.rows.PlaylistRow
import cc.tomko.outify.ui.components.user.UserChipAvatar
import cc.tomko.outify.ui.screens.MaterialSearchBar
import cc.tomko.outify.ui.viewmodel.library.LibraryViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.LibraryScreen(
    viewModel: LibraryViewModel,
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val playlists by viewModel.playlists.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadPlaylistUris()
    }

    val density = LocalDensity.current
    val sharedTransitionScope = this

    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val collapsingState = rememberCollapsingHeaderState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()

    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 2 ||
            lazyListState.firstVisibleItemScrollOffset > 100
        }
    }
    val showScrollToTop = isScrolled

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) playlists
        else playlists.filter { playlist ->
            playlist.attributes.name.contains(searchQuery, ignoreCase = true) ||
            playlist.ownerUsername.contains(searchQuery, ignoreCase = true)
        }
    }

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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialSearchBar(
                        onQueryChange = { searchQuery = it },
                        isLoading = false,
                        autoFocus = false,
                        placeholderText = "Search playlists..",
                    )
                }
            }

            items(
                items = filteredPlaylists,
                key = { it.uri },
                contentType = { "playlist" }
            ) { playlist ->
                val artworkUrl by produceState<String?>(
                    initialValue = null,
                    key1 = playlist.uri
                ) {
                    value = viewModel.getArtworkUrl(playlist)
                }

                val authors by produceState(
                    initialValue = emptyList(),
                    key1 = playlist.uri
                ) {
                    value = viewModel.getAuthors(playlist).take(3)
                }

                PlaylistRow(
                    playlist = playlist,
                    artworkUrl = artworkUrl,
                    onRowClick = {
                        backStack.add(Route.PlaylistScreen(playlist.uri))
                    },
                    onRowLongClick = {
                        GlobalPopupController.show(PopupSpec.PlaylistInfo(playlist, artworkUrl))
                    },
                    trailingContent = {
                        authors.forEach {
                            UserChipAvatar(
                                artworkUrl = it.imageUrl,
                                modifier = Modifier
                                    .clickable {
                                        backStack.add(Route.ProfileScreen(it.uri))
                                    })
                        }
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

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    }
}