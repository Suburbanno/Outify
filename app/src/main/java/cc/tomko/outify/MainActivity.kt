package cc.tomko.outify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import cc.tomko.outify.MainActivity.MainActivity.LocalAnimatedVisibilityScope
import cc.tomko.outify.MainActivity.MainActivity.LocalSharedTransitionScope
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.ui.components.navigation.NavDestination
import cc.tomko.outify.ui.components.navigation.NavigationRoot
import cc.tomko.outify.ui.components.navigation.OutifyBottomNav
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.components.player.MiniPlayer
import cc.tomko.outify.ui.screens.auth.AuthActivity
import cc.tomko.outify.ui.theme.OutifyTheme

val MiniPlayerHeight = 80.dp
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

        if(!handleAuth()){
            return;
        }

        startServices()
    }

    @Composable
    fun App(){
        val backStack = rememberNavBackStack(Route.HomeScreen)

        val routes = listOf(
            NavDestination("home", "Home", Route.HomeScreen) { Icon(Icons.Default.Home, contentDescription = null) },
            NavDestination("search", "Search", Route.SearchScreen) { Icon(Icons.Default.Search, contentDescription = null) },
            NavDestination("liked", "Liked", Route.LikedScreen) { Icon(Icons.Default.Favorite, contentDescription = null) },
        )

        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this
            ) {
                Scaffold(
                    bottomBar = {
                        Column {
                            MiniPlayer(
                                backStack = backStack,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            )

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
    fun startServices() {
        // Starting Spirc
        val spirc = Spirc()
        if(!spirc.initializeSpirc()){
            // How do we handle this?
            return;
        }
        // Spirc activation and transfer is handled in initializeSpirc -> callback to onSpircInitialize

        OutifyApplication.spirc = spirc
    }
}