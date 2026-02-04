package cc.tomko.outify.ui.components.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.library.LibraryScreen
import cc.tomko.outify.ui.viewmodel.LibraryViewModel

@Composable
fun NavigationRoot(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
        ),
//        transitionSpec = {
//            slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it })
//        },
//        popTransitionSpec = {
//            slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it })
//        },
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
                        val app = LocalContext.current.applicationContext as OutifyApplication?
                        val viewModel = remember { LibraryViewModel(app!!) }
                        val listState = rememberLazyListState()

                        LibraryScreen(
                            viewModel = viewModel,
                            listState = listState,
                            onTrackClick = { track ->
                                OutifyApplication.spirc.load(track.uri)
                            }
                        )
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}