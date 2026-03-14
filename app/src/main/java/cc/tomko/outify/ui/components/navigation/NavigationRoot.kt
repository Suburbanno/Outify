package cc.tomko.outify.ui.components.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.auth.AuthScreen
import cc.tomko.outify.ui.screens.auth.SetupOutifyScreen
import cc.tomko.outify.ui.screens.library.LibraryScreen
import cc.tomko.outify.ui.screens.library.LikedScreen
import cc.tomko.outify.ui.screens.library.PlaylistScreen
import cc.tomko.outify.ui.screens.library.album.AlbumDetailScreen
import cc.tomko.outify.ui.screens.library.artist.ArtistDetailScreen
import cc.tomko.outify.ui.screens.search.SearchScreen
import cc.tomko.outify.ui.screens.settings.AppearanceSettingScreen
import cc.tomko.outify.ui.screens.settings.GestureSettingsScreen
import cc.tomko.outify.ui.screens.settings.InterfaceSettingScreen
import cc.tomko.outify.ui.screens.settings.PlaybackSettingScreen
import cc.tomko.outify.ui.screens.settings.SettingsScreen
import cc.tomko.outify.ui.viewmodel.SearchViewModel
import cc.tomko.outify.ui.viewmodel.auth.AuthViewModel
import cc.tomko.outify.ui.viewmodel.auth.SetupViewModel
import cc.tomko.outify.ui.viewmodel.library.ArtistViewModel
import cc.tomko.outify.ui.viewmodel.library.LibraryViewModel
import cc.tomko.outify.ui.viewmodel.library.LikedViewModel
import cc.tomko.outify.ui.viewmodel.library.PlaylistViewModel
import cc.tomko.outify.ui.viewmodel.library.album.AlbumViewModel
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel
import cc.tomko.outify.ui.viewmodel.settings.AppearanceViewModel
import cc.tomko.outify.ui.viewmodel.settings.GestureSettingViewModel
import cc.tomko.outify.ui.viewmodel.settings.InterfaceViewModel
import cc.tomko.outify.ui.viewmodel.settings.PlaybackSettingViewModel
import cc.tomko.outify.ui.viewmodel.settings.SettingsViewModel

@Composable
fun SharedTransitionScope.NavigationRoot(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
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
        entryProvider = entryProvider {
            entry<Route.LibrespotAuthScreen> {
                val viewModel: AuthViewModel = hiltViewModel()

                viewModel.setNavigateCallback { route -> backStack.add(route) }
                viewModel.setProgress(it.progress)

                AuthScreen(
                    viewModel = viewModel,
                )
            }
            entry<Route.SetupScreen> {
                val viewModel: SetupViewModel = hiltViewModel()
                viewModel.onSetupComplete = {
                    backStack.clear()
                    backStack.add(Route.HomeScreen)
                }

                SetupOutifyScreen(
                    viewModel = viewModel
                )
            }

            entry<Route.HomeScreen> {
                HomeScreen(
                    backStack
                )
            }
            entry<Route.PlayerScreen>(
                metadata = verticalTransition()
            ) {
                val viewModel: PlayerViewModel = hiltViewModel()

                PlayerScreen(
                    viewModel = viewModel,
                    onArtistClick = {
                        backStack.add(Route.ArtistScreen(it.uri))
                    },
                    onMoreOptions = {
                        GlobalPopupController.showTrackPopup(viewModel.state.value.currentTrack!!)
                    }
                )
            }

            entry<Route.LikedScreen>  {
                val viewModel: LikedViewModel = hiltViewModel()
                val listState = rememberLazyListState()

                LikedScreen(
                    viewModel = viewModel,
                    listState = listState,
                    onBack = {
                        backStack.removeAt(backStack.lastIndex)
                    },
                    onArtworkClick = {
                        backStack.add(Route.AlbumScreenFromAlbumUri(it.uri))
                    },
                    onArtistClick = {
                        backStack.add(Route.ArtistScreen(it.uri))
                    }
                )
            }

            entry<Route.LibraryScreen> {
                val viewModel: LibraryViewModel = hiltViewModel()

                LibraryScreen(viewModel, backStack)
            }

            entry<Route.SearchScreen> {
                val viewModel = hiltViewModel<SearchViewModel>()
                SearchScreen(backStack,viewModel)
            }

            entry<Route.AlbumScreenFromTrackUri> {
                val viewModel: AlbumViewModel = hiltViewModel()

                LaunchedEffect(it.trackUri) {
                    viewModel.loadAlbumFromTrackUri(it.trackUri)
                }

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

            entry<Route.AlbumScreenFromAlbumUri> {
                val viewModel: AlbumViewModel = hiltViewModel()

                LaunchedEffect(it.albumUri) {
                    viewModel.loadAlbum(it.albumUri)
                }

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

            entry<Route.ArtistScreen> {
                val viewModel: ArtistViewModel = hiltViewModel()
                LaunchedEffect(viewModel) {
                    viewModel.loadArtist(it.artistUri)
                }

                ArtistDetailScreen(
                    viewModel,
                    onArtworkClick = { track ->
                        backStack.add(Route.AlbumScreenFromTrackUri(track.uri))
                    },
                    onAlbumClick = { album ->
                        backStack.add(Route.AlbumScreenFromAlbumUri(album.uri))
                    },
                    onArtistClick = { backStack.add(Route.ArtistScreen(it.uri)) }
                ) { }
            }

            entry<Route.PlaylistScreen> {
                val viewModel: PlaylistViewModel = hiltViewModel()
                viewModel.loadPlaylist(it.playlistUri, false)

                PlaylistScreen(
                    viewModel = viewModel,
                    onBack = {
                        backStack.removeAt(backStack.lastIndex)
                    },
                    onArtworkClick = { track ->
                        backStack.add(Route.AlbumScreenFromTrackUri(track.uri))
                    },
                    onArtistClick = { backStack.add(Route.ArtistScreen(it.uri)) }
                )
            }

            entry<Route.SettingsScreen> {
                val viewModel: SettingsViewModel = hiltViewModel()

                SettingsScreen(
                    viewModel = viewModel,
                    openInterfaceSettings = {
                        backStack.add(Route.InterfaceSettings)
                    },
                    openPlaybackSettings = {
                        backStack.add(Route.PlaybackSettings)
                    },
                    openDebugSettings = {
                        backStack.add(Route.SetupScreen)
                    }
                )
            }

            entry<Route.InterfaceSettings> {
                val viewModel: InterfaceViewModel = hiltViewModel()

                InterfaceSettingScreen(
                    viewModel = viewModel,
                    openGestureSettings = {
                        backStack.add(Route.GestureSettings)
                    },
                    openAppearanceSettings = {
                        backStack.add(Route.AppearanceSettings)
                    },
                )
            }

            entry<Route.AppearanceSettings> {
                val viewModel: AppearanceViewModel = hiltViewModel()

                AppearanceSettingScreen(
                    viewModel = viewModel,
                )
            }

            entry<Route.GestureSettings> {
                val viewModel: GestureSettingViewModel = hiltViewModel()

                GestureSettingsScreen(
                    viewModel = viewModel
                )
            }

            entry<Route.PlaybackSettings> {
                val viewModel: PlaybackSettingViewModel = hiltViewModel()

                PlaybackSettingScreen(
                    viewModel = viewModel
                )
            }
        }
    )
}

fun verticalTransition() = NavDisplay.transitionSpec {
    val enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn()
    val exit = slideOutVertically(targetOffsetY = { fullHeight -> -fullHeight }) + fadeOut()

    enter togetherWith exit
} + NavDisplay.popTransitionSpec {
    val enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight }) + fadeIn()
    val exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()

    enter togetherWith exit
} + NavDisplay.predictivePopTransitionSpec {
    val enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight }) + fadeIn()
    val exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()

    enter togetherWith exit
}
