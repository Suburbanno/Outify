package cc.tomko.outify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.screens.HomeScreen
import cc.tomko.outify.ui.screens.PlayerScreen
import cc.tomko.outify.ui.screens.auth.AuthActivity
import cc.tomko.outify.ui.theme.OutifyTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OutifyTheme {
                Surface {
                    PlayerScreen(OutifyApplication.playbackManager.playbackStateHolder)
                }
            }
        }

        if(!handleAuth()){
            return;
        }

        startServices()
    }

    // Checks for existing credentials and redirects to the login page if needed
    fun handleAuth(): Boolean {
        val authMan = OutifyApplication.spAuthManager;

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

        spirc.load("spotify:track:1BDRKVuooLvqayamtAEV4z")

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