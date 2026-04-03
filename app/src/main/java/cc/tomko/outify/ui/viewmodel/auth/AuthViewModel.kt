package cc.tomko.outify.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import cc.tomko.outify.core.AuthCallbackServer
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.ui.components.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authManager: AuthManager,
): ViewModel() {
    private var server: AuthCallbackServer? = null

    private lateinit var navigateCallback: (Route) -> Unit
    private val _progress = MutableStateFlow(LibrespotAuthProgress.START)
    val progress: StateFlow<LibrespotAuthProgress> = _progress

    fun startAuth(onCodeReceived: (String, String?) -> Unit) {
        server = AuthCallbackServer(onCodeReceived = onCodeReceived)
        server?.start()
    }

    fun verifyCode(code: String, state: String?) {
        val success = authManager.handleOAuthCode(code, state)
        _progress.value = if (success) LibrespotAuthProgress.SUCCESS else LibrespotAuthProgress.FAILED
        server?.stop()
        server = null
    }

    fun navigateToImport(){
        navigateCallback(Route.SetupScreen)
    }

    fun restartAuth(){
        _progress.value = LibrespotAuthProgress.START
    }

    fun oauthUrl(): String {
        return authManager.getAuthorizationURL()
    }

    fun setProgress(progress: LibrespotAuthProgress){
        _progress.value = progress
    }

    fun setNavigateCallback(callback: (Route) -> Unit){
        navigateCallback = callback
    }
}

enum class LibrespotAuthProgress {
    START,
    SUCCESS,
    FAILED
}