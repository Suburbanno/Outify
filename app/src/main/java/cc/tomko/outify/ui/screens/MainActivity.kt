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
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.SecureStorage
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

        handleAuth();
    }

    fun handleAuth() {
        val authMan = OutifyApplication.spAuthManager;

        if (!authMan.isAuthenticated()) {
            startActivity(Intent(this, AuthActivity::class.java));
            finish();
        }

        val accessToken =
            OutifyApplication.secureStorage.getString(SecureStorage.Keys.ACCESS_TOKEN);
        val callback: SessionInitializationCallback = object : SessionInitializationCallback {
            override fun onConnected() {
                Log.i("MainActivity", "onConnected: Session initialized");
                OutifyApplication.audioManager.initializePlayer();
                OutifyApplication.audioManager.playTrack("2WUy2Uywcj5cP0IXQagO3z");
            }

            override fun onError(message: String?) {
                Log.e("MainActivity", "onError: Callback failed with: " + message,)
            }
        }

        OutifyApplication.audioManager.initializeSession(accessToken, callback);
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