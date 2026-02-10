package cc.tomko.outify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.rememberNavBackStack
import cc.tomko.outify.MainActivity.MainActivity.LocalAnimatedVisibilityScope
import cc.tomko.outify.MainActivity.MainActivity.LocalSharedTransitionScope
import cc.tomko.outify.core.spirc.Spirc
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
import cc.tomko.outify.ui.viewmodel.player.QueueViewModel

class MainActivity : ComponentActivity() {

    data object MainActivity {
        var LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> { error("No scope provided") }
        var LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope> { error("No scope provided") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OutifyTheme(
                content = {
                    App()
                }
            )
        }

        requestNotifications()

        if(!handleAuth()){
            return;
        }

        startServices()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun App(){
        val backStack = rememberNavBackStack(Route.HomeScreen)

        val routes = listOf(
            NavDestination("home", "Home", Route.HomeScreen) { Icon(Icons.Default.Home, contentDescription = null) },
            NavDestination("search", "Search", Route.SearchScreen) { Icon(Icons.Default.Search, contentDescription = null) },
            NavDestination("liked", "Liked", Route.LikedScreen) { Icon(Icons.Default.Favorite, contentDescription = null) },
        )

        val sheetState = rememberQueueBottomSheetState()
//        val currentTrack by OutifyApplication.playbackManager.playbackStateHolder.currentTrack.collectAsState()
        val queueViewModel = remember { QueueViewModel(application) }

        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this
            ) {
                Scaffold(
                    bottomBar = {
                        Column {
                            AnimatedVisibility(
//                                visible = currentTrack != null,
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight }
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight -> fullHeight }
                                ) + fadeOut()
                            ) {
                                MiniPlayer(
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
        val authMan = OutifyApplication.authManager;

        if (!authMan.hasCachedCredentials()) {
            startActivity(Intent(this, AuthActivity::class.java));
            finish();
            return false;
        }
        return true;
    }

    // Starts the required services
    @androidx.annotation.OptIn(UnstableApi::class)
    fun startServices() {
        // Starting Spirc
        val spirc = Spirc()
        if(!spirc.initializeSpirc()){
            // How do we handle this?
            return;
        }
        // Spirc activation and transfer is handled in initializeSpirc -> callback to onSpircInitialize

        OutifyApplication.spirc = spirc

        // MediaSession
        val intent = Intent(this, MusicService::class.java)
        ContextCompat.startForegroundService(this, intent)
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
}