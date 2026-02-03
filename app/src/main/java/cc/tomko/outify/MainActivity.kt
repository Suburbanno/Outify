package cc.tomko.outify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.model.LikedTrack
import cc.tomko.outify.ui.repository.LibraryRepository
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.library.LibraryScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.auth.AuthActivity
import cc.tomko.outify.ui.screens.search.SearchOverlay
import cc.tomko.outify.ui.theme.OutifyTheme
import cc.tomko.outify.ui.viewmodel.LibraryViewModel

val MiniPlayerHeight = 80.dp
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OutifyTheme {
                Surface {
                    val navController = rememberNavController()

                    MainScaffold(
                        navController = navController
                    )
                }
            }
        }

        if(!handleAuth()){
            return;
        }

        startServices()
    }

    @Composable
    fun MainScaffold(
        navController: NavHostController,
    ) {
        val app = (application as OutifyApplication)

        var searchActive by rememberSaveable { mutableStateOf(false) }
        var query by rememberSaveable { mutableStateOf("") }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("library") }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search"
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "player",
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable("home") { HomeScreen() }
                    composable("library") {
                        val viewModel = remember { LibraryViewModel(app) }

                        LibraryScreen(
                            viewModel = viewModel,
                            onTrackClick = {  track:  Track ->
                                OutifyApplication.spirc.load(track.uri)
                            }
                        )
                    }

                    composable("player") { PlayerScreen(OutifyApplication.playbackManager.playbackStateHolder) }
                }

                SearchOverlay(
                    active = searchActive,
                    query = query,
                    onQueryChange = { query = it },
                    onClose = {
                        searchActive = false
                        query = ""
                    },
                )
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OutifyTheme {
        Greeting("Android")
    }
}