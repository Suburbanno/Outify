package cc.tomko.outify.ui.screens

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.core.model.Album
import cc.tomko.outify.core.model.Artist
import cc.tomko.outify.core.model.OutifyUri
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.toSpotifyUri
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.SmartImage
import cc.tomko.outify.ui.components.rows.SwipeableTrackRowConfigured
import cc.tomko.outify.ui.viewmodel.HomeUiState
import cc.tomko.outify.ui.viewmodel.HomeViewModel
import cc.tomko.outify.ui.viewmodel.TopArtist

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val username by viewModel.username.collectAsState(initial = "User")
    val userAvatarUrl by viewModel.userImageUrl.collectAsState(initial = null)

    Scaffold(
        modifier = modifier,
    ) { innerPaddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPaddings.calculateTopPadding())
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.NotAuthenticated -> {
                    println(userAvatarUrl)
                    NotAuthenticatedContent(
                        username = username,
                        onSettingsClick = {
                            backStack.add(Route.SettingsScreen)
                        },
                        onAccountClick = {
                            backStack.add(Route.AccountsScreen)
                        }
                    )
                }

                is HomeUiState.Success -> {
                    val currentTrack by viewModel.currentTrack.collectAsState(initial = null)
                    val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)

                    HomeContent(
                        username = username,
                        userAvatarUrl = userAvatarUrl,
                        topArtists = state.topArtists,
                        topTracks = state.topTracks,
                        onSettingsClick = {
                            backStack.add(Route.SettingsScreen)
                        },
                        onAccountClick = {
                            backStack.add(Route.AccountsScreen)
                        },
                        onArtistClick = {
                            backStack.add(Route.ArtistScreen(it))
                        },
                        currentTrack = currentTrack,
                        isPlaybackPlaying = isPlaybackPlaying,
                        onArtworkClick = {
                            backStack.add(Route.AlbumScreen(it.uri))
                        },
                        onTrackClick = {
                            viewModel.loadTrack(it)
                        },
                    )
                }

                is HomeUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onSettingsClick = {
                            backStack.add(Route.SettingsScreen)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotAuthenticatedContent(
    username: String?,
    onSettingsClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome back,\n${username ?: "User"}!",
                style = MaterialTheme.typography.headlineLargeEmphasized,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onAccountClick) {
                Icon(Icons.Default.NoAccounts, contentDescription = null)
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Connect to Spotify",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Link your Spotify account to see your top artists and tracks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onAccountClick()
                    },
                    shape = RoundedCornerShape(50),
                ) {
                    Text("Connect Spotify")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SharedTransitionScope.HomeContent(
    username: String?,
    userAvatarUrl: String?,
    currentTrack: Track?,
    isPlaybackPlaying: Boolean,
    topArtists: List<TopArtist>,
    topTracks: List<Track>,
    onSettingsClick: () -> Unit,
    onAccountClick: () -> Unit,
    onArtistClick: (uri: String) -> Unit,
    onArtworkClick: (album: Album) -> Unit,
    onTrackClick: (track: Track) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome back,\n${username ?: "User"}!",
                    style = MaterialTheme.typography.headlineLargeEmphasized,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onAccountClick) {
                    SmartImage(
                        url = userAvatarUrl
                    )
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }

        if (topArtists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Top Artists",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(topArtists.take(10)) { artist ->
                        TopArtistItem(
                            artist = artist,
                            modifier = Modifier.clickable {
                                onArtistClick(artist.uri)
                            })
                    }
                }
            }
        }

        if (topTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Top Tracks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            items(topTracks.take(10)) { track ->
                SwipeableTrackRowConfigured(
                    track,
                    currentTrack = currentTrack,
                    isPlaybackPlaying = isPlaybackPlaying,
                    onRowClick = remember(track.uri) {
                        {
                            onTrackClick(track)
                        }
                    },
                    onArtworkClick = {
                        onArtworkClick(track.album!!)
                    },
                    onArtistClick = { artist ->
                        onArtistClick(artist.uri)
                    },
                    trailingContent = {
                        Text(
                            text = formatDuration(track.duration.toInt()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun TopArtistItem(artist: TopArtist, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (artist.imageUrl != null) {
                SmartImage(
                    url = artist.imageUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.name.take(1),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ErrorContent(
    message: String,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.headlineLargeEmphasized,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun formatDuration(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}