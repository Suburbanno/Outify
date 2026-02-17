package cc.tomko.outify.ui.screens.library

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.ui.components.PlaylistRow
import cc.tomko.outify.ui.components.navigation.Route
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
    val lazyListState = rememberLazyListState()

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
            LazyColumn(
                state = lazyListState
            ) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Your library", style = MaterialTheme.typography.headlineLargeEmphasized, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))

                    Text("${playlists.count()} songs", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 24.dp, bottom = 4.dp))
                    Spacer(Modifier.height(12.dp))
                }

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
                        playlist,
                        artworkUrl,
                        onRowClick = {
                            backStack.add(Route.PlaylistScreen(playlist.uri))
                        }
                    )
                }
            }
        }
    }
}