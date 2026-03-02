package cc.tomko.outify.ui.screens.auth

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.ui.theme.OutifyTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

/**
 * Used for OAuth flow code retrieval
 */
@AndroidEntryPoint
class CallbackActivity : ComponentActivity() {
    @Inject lateinit var authManager: AuthManager

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

        val codeSuccess = authManager.handleOAuthCode(code, state);
        // TODO: Handle more appropriately
        if (codeSuccess) {
            return StringArrayResult.Success("Credentials saved!")
        }

        return StringArrayResult.Error("An error occurred")
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