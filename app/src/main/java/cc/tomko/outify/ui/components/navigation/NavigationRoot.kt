package cc.tomko.outify.ui.components.navigation

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.library.LikedScreen
import cc.tomko.outify.ui.screens.search.SearchScreen
import cc.tomko.outify.ui.viewmodel.LikedViewModel
import cc.tomko.outify.ui.viewmodel.SearchViewModel

@Composable
fun NavigationRoot(
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
                        val viewModel = remember { LikedViewModel(app!!) }
                        val listState = rememberLazyListState()

                        LikedScreen(
                            viewModel = viewModel,
                            listState = listState,
                            onTrackClick = { track ->
                                OutifyApplication.spirc.load(track.uri)
                            }
                        )
                    }
                }

                is Route.SearchScreen -> {
                    NavEntry(key) {
                        val viewModel = remember { SearchViewModel(app!!, app.metadata) }
                        SearchScreen(viewModel)
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}