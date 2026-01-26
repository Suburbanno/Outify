package cc.tomko.outify.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cc.tomko.outify.Debug
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.SecureStorage
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.playback.AudioManager
import cc.tomko.outify.playback.SessionInitializationCallback
import cc.tomko.outify.profile.UserProfile
import cc.tomko.outify.ui.screens.auth.AuthActivity
import cc.tomko.outify.ui.theme.OutifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OutifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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