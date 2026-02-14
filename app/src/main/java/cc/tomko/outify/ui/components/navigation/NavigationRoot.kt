package cc.tomko.outify.ui.components.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.navDeepLink
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.library.LikedScreen
import cc.tomko.outify.ui.screens.library.album.AlbumDetailScreen
import cc.tomko.outify.ui.screens.library.artist.ArtistDetailScreen
import cc.tomko.outify.ui.screens.library.artist.ArtistLikedTracksScreen
import cc.tomko.outify.ui.screens.search.SearchScreen
import cc.tomko.outify.ui.viewmodel.library.LikedViewModel
import cc.tomko.outify.ui.viewmodel.SearchViewModel
import cc.tomko.outify.ui.viewmodel.library.ArtistViewModel
import cc.tomko.outify.ui.viewmodel.library.album.AlbumViewModel
import cc.tomko.outify.ui.viewmodel.library.album.AlbumViewModelFactory

@Composable
fun SharedTransitionScope.NavigationRoot(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as OutifyApplication?

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
        ),
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        },
        popTransitionSpec = {
            slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
        },
        predictivePopTransitionSpec = {
            slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
        },
        entryProvider = { key ->
            when (key) {
                is Route.HomeScreen -> {
                    NavEntry(key) {
                        HomeScreen()
                    }
                }
                is Route.PlayerScreen -> {
                    NavEntry(key) {
                        PlayerScreen(OutifyApplication.playbackStateHolder)
                    }
                }

                is Route.LikedScreen -> {
                    NavEntry(key) {
                        val viewModel: LikedViewModel = viewModel()
                        val listState = rememberLazyListState()

                        LikedScreen(
                            viewModel = viewModel,
                            listState = listState,
                            backStack = backStack,
                        )
                    }
                }

                is Route.SearchScreen -> {
                    NavEntry(key) {
                        val viewModel = remember { SearchViewModel(app!!, app.metadata) }
                        SearchScreen(backStack,viewModel)
                    }
                }

                is Route.AlbumScreenFromTrackUri -> {
                    NavEntry(key) {
                        val context = LocalContext.current.applicationContext as OutifyApplication
                        val track by produceState<Track?>(initialValue = null, key1 = key.trackUri) {
                            value = context.metadata.getTrackMetadata(listOf(key.trackUri)).firstOrNull()
                        }

                        val albumUri = track?.album?.uri ?: ""
                        val factory = AlbumViewModelFactory(
                            application = context,
                            albumUri = albumUri
                        )

                        val viewModel: AlbumViewModel = viewModel(
                            key = albumUri,
                            factory = factory
                        )

                        AlbumDetailScreen(
                            viewModel = viewModel,
                            onBack = {
                                backStack.removeAt(backStack.lastIndex)
                            },
                            artistClick = { uri ->
                                backStack.add(Route.ArtistScreen(uri))
                            }
                        )
                    }
                }

                is Route.AlbumScreenFromAlbumUri -> {
                    NavEntry(key) {
                        val context = LocalContext.current.applicationContext as OutifyApplication

                        val albumUri = key.albumUri
                        val factory = AlbumViewModelFactory(
                            application = context,
                            albumUri = albumUri
                        )

                        val viewModel: AlbumViewModel = viewModel(
                            key = albumUri,
                            factory = factory
                        )

                        AlbumDetailScreen(
                            viewModel = viewModel,
                            onBack = {
                                backStack.removeAt(backStack.lastIndex)
                            },
                            artistClick = { uri ->
                                backStack.add(Route.ArtistScreen(uri))
                            }
                        )
                    }
                }

                is Route.ArtistScreen -> {
                    NavEntry(key) {
                        val viewModel: ArtistViewModel = hiltViewModel()
                        LaunchedEffect(viewModel) {
                            viewModel.loadArtist(key.artistUri)
                        }

                        ArtistDetailScreen(
                            viewModel,
                            onArtworkClick = { track ->
                                backStack.add(Route.AlbumScreenFromTrackUri(track.uri))
                            },
                            onLikedTracksClick = {
                                backStack.add(Route.ArtistLikedTracksScreen(key.artistUri))
                            }
                        ) { }
                    }
                }

                is Route.ArtistLikedTracksScreen -> {
                    NavEntry(key) {
                        val viewModel: ArtistViewModel = hiltViewModel()
                        LaunchedEffect(viewModel) {
                            viewModel.loadArtist(key.artistUri)
                        }

                        ArtistLikedTracksScreen(
                            viewModel,
                            onArtworkClick = { track ->
                                backStack.add(Route.AlbumScreenFromTrackUri(track.uri))
                            },
                        ) { }
                    }
                }

                else -> error("Unknown NavKey: $key")
            }
        }
    )
}