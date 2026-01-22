package cc.tomko.outify.ui.screens

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.SecureStorage
import cc.tomko.outify.ui.theme.OutifyTheme

class CallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // handle intent immediately
        val intentUri = intent?.data
        val tokensResult = handleIntent(intentUri)

        // Compose UI
        setContent {
            OutifyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CallbackScreen(tokensResult)
                }
            }
        }
    }

    private fun handleIntent(uri: Uri?): StringArrayResult? {
        if (uri == null || uri.scheme != "outify") return null
        if (uri.host == "oauth" && uri.path == "/verify") {
            return handleOAuthVerify(uri)
        }
        return null
    }

    private fun handleOAuthVerify(uri: Uri): StringArrayResult {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        if (code == null || state == null) return StringArrayResult.Error("Invalid OAuth callback")

        val dto = OutifyApplication.spAuthManager.getTokenData(code, state);

        // Saving auth credentials
        return try {
            val storage = OutifyApplication.secureStorage;

            storage.putString(SecureStorage.Keys.ACCESS_TOKEN, dto.accessToken);
            storage.putString(SecureStorage.Keys.REFRESH_TOKEN, dto.refreshToken);
            storage.putObject<Long>(SecureStorage.Keys.ACCESS_TOKEN_EXPIRATION, dto.expiresAt);
            StringArrayResult.Success("Credentials saved!")
        } catch (e: Exception) {
            StringArrayResult.Error("An error occurred")
        }
    }
}

sealed class StringArrayResult {
    data class Success(val message: String) : StringArrayResult()
    data class Error(val message: String) : StringArrayResult()
}

@Composable
fun CallbackScreen(result: StringArrayResult?) {
    val context = LocalContext.current

    // Only show Toast once when result changes
    LaunchedEffect(result) {
        when (result) {
            is StringArrayResult.Success -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            is StringArrayResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            null -> Unit
        }
    }
}