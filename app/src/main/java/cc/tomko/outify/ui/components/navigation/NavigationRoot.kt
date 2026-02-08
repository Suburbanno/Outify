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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.library.LikedScreen
import cc.tomko.outify.ui.screens.library.album.AlbumDetailScreen
import cc.tomko.outify.ui.screens.library.album.AlbumScreen
import cc.tomko.outify.ui.screens.search.SearchScreen
import cc.tomko.outify.ui.viewmodel.library.LikedViewModel
import cc.tomko.outify.ui.viewmodel.SearchViewModel
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
                        PlayerScreen(OutifyApplication.playbackManager.playbackStateHolder)
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
                            onTrackClick = { track ->
                                OutifyApplication.spirc.load(track.uri)
                            }
                        )
                    }
                }

                is Route.SearchScreen -> {
                    NavEntry(key) {
                        val viewModel = remember { SearchViewModel(app!!, app.metadata) }
                        SearchScreen(backStack,viewModel)
                    }
                }

                is Route.AlbumScreen -> {
                    NavEntry(key) {
                        val context = LocalContext.current.applicationContext as OutifyApplication
                        val factory = AlbumViewModelFactory(
                            application = context,
                            albumUri = key.albumUri
                        )

                        val viewModel: AlbumViewModel = viewModel(
                            key = key.albumUri,
                            factory = factory
                        )

                        AlbumScreen(
                            viewModel = viewModel,
                        )
                    }
                }

                is Route.AlbumScreenFromTrack -> {
                    NavEntry(key) {
                        val context = LocalContext.current.applicationContext as OutifyApplication
                        val albumUri = key.track.album?.uri ?: ""
                        println(albumUri)
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
                            }
//                            sourceTrack = key.track
                        )
                    }
                }

                else -> error("Unknown NavKey: $key")
            }
        }
    )
}