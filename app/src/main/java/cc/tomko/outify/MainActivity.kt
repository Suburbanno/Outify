package cc.tomko.outify

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import cc.tomko.outify.MainActivity.MainActivity.LocalSharedTransitionScope
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.data.setting.LocalSwipeActionHandler
import cc.tomko.outify.data.setting.LocalSwipeGestureSettings
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.components.GlobalPopupHost
import cc.tomko.outify.ui.components.navigation.NavDestination
import cc.tomko.outify.ui.components.navigation.NavigationRoot
import cc.tomko.outify.ui.components.navigation.OutifyBottomNav
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.player.MiniPlayer
import cc.tomko.outify.ui.components.player.PlayerSheet
import cc.tomko.outify.ui.components.player.QueueBottomSheet
import cc.tomko.outify.ui.components.player.rememberPlayerSheetState
import cc.tomko.outify.ui.components.player.rememberQueueBottomSheetState
import cc.tomko.outify.ui.notifications.InAppNotificationHost
import cc.tomko.outify.data.repository.InterfaceSettings
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.OutifyTheme
import cc.tomko.outify.ui.PopupSpec
import cc.tomko.outify.ui.viewmodel.MainViewModel
import cc.tomko.outify.ui.viewmodel.bottomsheet.AddToPlaylistViewModel
import cc.tomko.outify.ui.viewmodel.bottomsheet.PlaybackDevicesViewModel
import cc.tomko.outify.ui.viewmodel.player.MiniPlayerViewModel
import cc.tomko.outify.ui.viewmodel.player.MultiQueueViewModel
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel
import cc.tomko.outify.ui.viewmodel.player.QueueViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authManager: AuthManager

    private val deepLinkFlow = MutableSharedFlow<Uri>(extraBufferCapacity = 1)

    data object MainActivity {
        val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> { error("No scope provided") }
        val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope> { error("No scope provided") }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val permissionCheck = checkSelfPermission(permission)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(permission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        intent?.data?.let {
            deepLinkFlow.tryEmit(it)
        }

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()

            App(mainViewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        intent.data?.let {
            deepLinkFlow.tryEmit(it)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun App(
        viewModel: MainViewModel,
    ) {
        val startRoute = Route.HomeScreen

        val backStack = rememberNavBackStack(startRoute)

        LaunchedEffect(Unit) {
            deepLinkFlow.collect { uri ->
                parseDeepLinkUriToNavKey(uri)?.let { navKey ->
                    backStack.add(navKey)
                }
            }
        }

        val routes = listOf(
            NavDestination("home", "Home", Route.HomeScreen) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null
                )
            },
            NavDestination("search", "Search", Route.SearchScreen) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null
                )
            },
            NavDestination("liked", "Liked", Route.LikedScreen()) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null
                )
            },
            NavDestination(
                "library",
                "Library",
                Route.LibraryScreen
            ) { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
        )

        val currentRoute = backStack.last()

        val selectedId = when (currentRoute) {
            Route.HomeScreen -> "home"
            Route.SearchScreen -> "search"
            Route.LikedScreen -> "liked"
            Route.LibraryScreen -> "library"
            else -> null
        }

        val sheetState = rememberQueueBottomSheetState()

        val queueViewModel: QueueViewModel = hiltViewModel()
        val multiQueueViewModel: MultiQueueViewModel = hiltViewModel()
        val miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
        val playerViewModel: PlayerViewModel = hiltViewModel()
        val addToPlaylistViewModel: AddToPlaylistViewModel = hiltViewModel()
        val playbackDevicesViewModel: PlaybackDevicesViewModel = hiltViewModel()

        val playerSheetState = rememberPlayerSheetState()
        val playerListState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val interfaceSettings by viewModel
            .interfaceSettings
            .collectAsState(initial = InterfaceSettings())

        val swipeSettings by viewModel.swipeSettings.collectAsState(initial = interfaceSettings.gestureSettings)
        val currentTrack by viewModel.currentTrack.collectAsState(initial = null)

        OutifyTheme(
            track = currentTrack,
            enableDynamicTheme = interfaceSettings.dynamicTheme,
            pureBlack = interfaceSettings.pureBlack,
            highContrastCompat = interfaceSettings.highContrastCompat,
            content = {
                SharedTransitionLayout {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides this,
                        LocalSwipeGestureSettings provides swipeSettings,
                        LocalSwipeActionHandler provides viewModel.swipeActionHandler,
                        LocalUiSettings provides interfaceSettings,
                    ) {
                        Scaffold(
                            bottomBar = {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { fullHeight -> fullHeight }
                                    ) + fadeIn(),
                                    exit = slideOutVertically(
                                        targetOffsetY = { fullHeight -> fullHeight }
                                    ) + fadeOut(),
                                ) {
                                    OutifyBottomNav(
                                        items = routes,
                                        selectedId = selectedId,
                                        onItemSelected = { item -> backStack.add(item.route) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                NavigationRoot(
                                    backStack,
                                    modifier = Modifier.matchParentSize(),
                                    bottomPadding = if (currentTrack != null) 88.dp else 0.dp
                                )

                                InAppNotificationHost(
                                    modifier = Modifier.matchParentSize(),
                                    maxWidthFraction = 0.92f
                                )

                                GlobalPopupHost(
                                    backStack = backStack,
                                    addToQueue = { viewModel.addToQueue(it.toUriString()) },
                                    playNext = { viewModel.playNext(it.toUriString()) },
                                    startRadio = { viewModel.startRadio(it) },
                                    openRadio = {
                                        val uri = viewModel.getRadioUri(it) ?: return@GlobalPopupHost
                                        backStack.add(Route.PlaylistScreen(uri))
                                    },
                                    addToPlaylist = { viewModel.addToPlaylist(it) },
                                    toggleLike = { viewModel.favorite(it.toUriString()) },

                                    addToPlaylistViewModel = addToPlaylistViewModel,
                                    playbackDevicesViewModel = playbackDevicesViewModel,
                                )

                                AnimatedVisibility(
                                    visible = currentTrack != null,
                                    enter = slideInVertically(
                                        initialOffsetY = { fullHeight -> fullHeight }
                                    ) + fadeIn(),
                                    exit = slideOutVertically(
                                        targetOffsetY = { fullHeight -> fullHeight }
                                    ) + fadeOut(),
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    PlayerSheet(
                                        sheetState = playerSheetState,
                                        listState = playerListState,
                                        miniPlayerHeight = 88.dp,
                                        miniContent = { progress ->
                                            MiniPlayer(
                                                viewModel = miniPlayerViewModel,
                                                onDismiss = {
                                                    miniPlayerViewModel.setTrack(null)
                                                },
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 12.dp
                                                ),
                                                showQueue = { sheetState.show() },
                                                onClick = {
                                                    scope.launch { playerSheetState.expand() }
                                                }
                                            )
                                        },
                                        fullContent = { progress ->
                                            PlayerScreen(
                                                viewModel = playerViewModel,
                                                listState = playerListState,
                                                onArtistClick = {
                                                    scope.launch {
                                                        playerSheetState.collapse()
                                                    }
                                                    backStack.add(Route.ArtistScreen(it.uri))
                                                },
                                                onMoreOptions = {
                                                    val isLiked = playerViewModel.isLiked.value
                                                    GlobalPopupController.show(PopupSpec.TrackInfo(currentTrack!!, action = {
                                                        scope.launch {
                                                            playerSheetState.collapse()
                                                        }
                                                    }, isLiked = isLiked))
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (sheetState.visible.value) {
                            QueueBottomSheet(
                                sheetState = sheetState.sheetState,
                                viewModel = queueViewModel,
                                multiQueueViewModel = multiQueueViewModel,
                                onArtistClick = {
                                    backStack.add(Route.ArtistScreen(it.uri))
                                },
                                onArtworkClick = {
                                    backStack.add(Route.TrackScreen(it.uri))
                                },
                                onDismissRequest = {
                                    sheetState.hide()
                                }
                            )
                        }
                    }
                }
            })
    }

    fun parseDeepLinkUriToNavKey(uri: Uri): NavKey? {
        // spotify:x:y (opaque)
        if (uri.scheme == "spotify" && uri.host == null) {
            val ssp = uri.schemeSpecificPart ?: return null
            val parts = ssp.split(":")
            if (parts.size == 2) {
                val type = parts[0]

                return when (type) {
                    "album" -> Route.AlbumScreen(uri.toString())
                    "artist" -> Route.ArtistScreen(uri.toString())
                    "track" -> Route.TrackScreen(uri.toString())
                    "playlist" -> Route.PlaylistScreen(uri.toString())
                    "user" -> Route.ProfileScreen(uri.toString())
                    else -> null
                }
            }
        }

        // spotify://x/y
        if (uri.scheme == "spotify" && uri.host != null) {
            val id = uri.lastPathSegment ?: return null
            val internalUri = "spotify:${uri.host}:$id"
            return when (uri.host) {
                "album" -> Route.AlbumScreen(internalUri)
                "artist" -> Route.ArtistScreen(internalUri)
                "track" -> Route.TrackScreen(internalUri)
                "playlist" -> Route.PlaylistScreen(internalUri)
                "user" -> Route.ProfileScreen(internalUri)
                else -> null
            }
        }

        // https://open.spotify.com/x/y
        if (uri.scheme == "https" && uri.host == "open.spotify.com") {
            val segments = uri.pathSegments
            if (segments.size >= 2) {
                val type = segments[0]
                val id = segments[1]
                val internalUri = "spotify:$type:$id"

                return when (type) {
                    "album" -> Route.AlbumScreen(internalUri)
                    "artist" -> Route.ArtistScreen(internalUri)
                    "track" -> Route.TrackScreen(internalUri)
                    "playlist" -> Route.PlaylistScreen(internalUri)
                    "user" -> Route.ProfileScreen(internalUri)
                    else -> null
                }
            }
        }

        return null
    }
}