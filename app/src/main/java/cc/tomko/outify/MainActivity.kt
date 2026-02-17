package cc.tomko.outify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import cc.tomko.outify.MainActivity.MainActivity.LocalAnimatedVisibilityScope
import cc.tomko.outify.MainActivity.MainActivity.LocalSharedTransitionScope
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.services.MusicService
import cc.tomko.outify.ui.components.navigation.NavDestination
import cc.tomko.outify.ui.components.navigation.NavigationRoot
import cc.tomko.outify.ui.components.navigation.OutifyBottomNav
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.player.MiniPlayer
import cc.tomko.outify.ui.components.player.QueueBottomSheet
import cc.tomko.outify.ui.components.player.rememberQueueBottomSheetState
import cc.tomko.outify.ui.screens.auth.AuthActivity
import cc.tomko.outify.ui.theme.OutifyTheme
import cc.tomko.outify.ui.viewmodel.MainViewModel
import cc.tomko.outify.ui.viewmodel.player.MiniPlayerViewModel
import cc.tomko.outify.ui.viewmodel.player.QueueViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authManager: AuthManager

    private val deepLinkFlow = MutableSharedFlow<Uri>(extraBufferCapacity = 1)

    data object MainActivity {
        var LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> { error("No scope provided") }
        var LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope> { error("No scope provided") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.data?.let {
            deepLinkFlow.tryEmit(it)
        }

        setContent {
            OutifyTheme(
                content = {
                    val mainViewModel: MainViewModel = hiltViewModel()
                    App(mainViewModel)
                }
            )
        }

        requestNotifications()

        if(!handleAuth()){
            return;
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
    @Composable
    fun App(
        viewModel: MainViewModel
    ){
        val backStack = rememberNavBackStack(Route.HomeScreen)

        LaunchedEffect(Unit) {
            deepLinkFlow.collect { uri ->
                parseDeepLinkUriToNavKey(uri)?.let { navKey ->
                    backStack.add(navKey)
                }
            }
        }

        val routes = listOf(
            NavDestination("home", "Home", Route.HomeScreen) { Icon(Icons.Default.Home, contentDescription = null) },
            NavDestination("search", "Search", Route.SearchScreen) { Icon(Icons.Default.Search, contentDescription = null) },
            NavDestination("liked", "Liked", Route.LikedScreen) { Icon(Icons.Default.Favorite, contentDescription = null) },
            NavDestination("library", "Library", Route.LibraryScreen) { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
        )

        val sheetState = rememberQueueBottomSheetState()

        val queueViewModel: QueueViewModel = hiltViewModel()
        val miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()

        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this
            ) {
                Scaffold(
                    bottomBar = {
                        Column {
                            val currentTrack by viewModel.currentTrack().collectAsState(initial = null)

                            AnimatedVisibility(
                                visible = currentTrack != null,
//                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight }
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight -> fullHeight }
                                ) + fadeOut()
                            ) {
                                MiniPlayer(
                                    viewModel = miniPlayerViewModel,
                                    backStack = backStack,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    showQueue = {
                                        sheetState.show()
                                    }
                                )
                            }

                            OutifyBottomNav(
                                items = routes,
                                selectedId = "home",
                                onItemSelected = { item -> backStack.add(item.route) }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavigationRoot(
                        backStack,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                if(sheetState.visible.value) {
                    QueueBottomSheet(
                        sheetState = sheetState.sheetState,
                        viewModel = queueViewModel,
                        onDismissRequest = {
                            sheetState.hide()
                        }
                    )
                }
            }
        }
    }

    // Checks for existing credentials and redirects to the login page if needed
    fun handleAuth(): Boolean {
        if (!authManager.hasCachedCredentials()) {
            startActivity(Intent(this, AuthActivity::class.java));
            finish();
            return false;
        }
        return true;
    }

    fun requestNotifications(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Permission already granted!", Toast.LENGTH_SHORT).show()
            } else {
                // Ask for permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Toast.makeText(this, "No need to request notification permission on this Android version", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show()
            }
        }


    fun parseDeepLinkUriToNavKey(uri: Uri): NavKey? {
        // spotify:x:y (opaque)
        if (uri.scheme == "spotify" && uri.host == null) {
            val ssp = uri.schemeSpecificPart ?: return null
            val parts = ssp.split(":")
            if (parts.size == 2) {
                val type = parts[0]

                return when (type) {
                    "album" -> Route.AlbumScreenFromAlbumUri(uri.toString())
                    "artist" -> Route.ArtistScreen(uri.toString())
                    "track" -> Route.AlbumScreenFromTrackUri(uri.toString())
                    else -> null
                }
            }
        }

        // spotify://x/y
        if (uri.scheme == "spotify" && uri.host != null) {
            val id = uri.lastPathSegment ?: return null
            val internalUri = "spotify:${uri.host}:$id"
            return when (uri.host) {
                "album" -> Route.AlbumScreenFromAlbumUri(internalUri)
                "artist" -> Route.ArtistScreen(internalUri)
                "track" -> Route.AlbumScreenFromTrackUri(internalUri)
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
                    "album" -> Route.AlbumScreenFromAlbumUri(internalUri)
                    "artist" -> Route.ArtistScreen(internalUri)
                    "track" -> Route.AlbumScreenFromTrackUri(internalUri)
                    else -> null
                }
            }
        }

        return null
    }
}