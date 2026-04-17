package cc.tomko.outify.ui.viewmodel.settings

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.AuthCallbackServer
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.UserProfile
import cc.tomko.outify.core.model.Profile
import cc.tomko.outify.data.metadata.NativeErrorHandler
import cc.tomko.outify.data.repository.SettingsRepository
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.PopupSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    val spClient: SpClient,
    val userProfile: UserProfile,
    val authManager: AuthManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private var server: AuthCallbackServer? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val _isPlaybackLoggedIn = MutableStateFlow(false)
    val isPlaybackLoggedIn: StateFlow<Boolean> = _isPlaybackLoggedIn.asStateFlow()

    private val _isAccountLoggedIn = MutableStateFlow(false)
    val isAccountLoggedIn: StateFlow<Boolean> = _isAccountLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userImageUrl = MutableStateFlow<String?>(null)
    val userImageUrl: StateFlow<String?> = _userImageUrl.asStateFlow()

    init {
        _isAccountLoggedIn.value = spClient.isOAuthAuthenticated()
        _isPlaybackLoggedIn.value = authManager.hasCachedCredentials()
        loadSavedUserProfile()
    }

    private fun loadSavedUserProfile() {
        viewModelScope.launch {
            _username.value = settingsRepository.username.first()
            _userImageUrl.value = settingsRepository.userImageUrl.first()
        }
    }

    fun startSpircAuth(context: Context) {
        server?.stop()

        server = AuthCallbackServer(onCodeReceived = { code, state ->
            val result = authManager.handleOAuthCode(code, state)
            val isSuccess = result.contains("\"success\":true")
            val errorDetails = if (!isSuccess) parseErrorMessage(result) else null
            if (!isSuccess) {
                NativeErrorHandler.handleErrorJson(result, "spirc oauth")
            }
            GlobalPopupController.show(PopupSpec.AuthResult(isSuccess, errorDetails = errorDetails))
            if (isSuccess) {
                _isPlaybackLoggedIn.value = true
            }
        })
        server?.start()

        val url = authManager.getAuthorizationURL()

        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
        )
    }

    fun startAccountAuth(context: Context) {
        server?.stop()

        server = AuthCallbackServer(onCodeReceived = { code, state ->
            val result = spClient.completeOAuthFlow(code)
            val isSuccess = result.contains("\"success\":true")
            val errorDetails = if (!isSuccess) parseErrorMessage(result) else null
            if (!isSuccess) {
                NativeErrorHandler.handleErrorJson(result, "account oauth")
            }
            GlobalPopupController.show(PopupSpec.AuthResult(isSuccess, errorDetails = errorDetails))
            if (isSuccess) {
                _isAccountLoggedIn.value = true
                fetchProfile()
            }
        })

        server?.start()

        val url = spClient.startOAuthFlow()

        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
        )
    }

    fun logoutPlayback() {
        authManager.logout()
        _isPlaybackLoggedIn.value = false
    }

    fun logoutAccount() {
        spClient.logout()
        _isAccountLoggedIn.value = false

        viewModelScope.launch {
            try {
                settingsRepository.removeUserProfile()
            } catch (e: Exception) {
            }
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            try {
                val userId = spClient.username()
                val profileJson = userProfile.getUserProfile(userId)
                var profileName: String? = null
                var profileImageUrl: String? = null

                if (profileJson != null) {
                    try {
                        val profile = json.decodeFromString<Profile>(profileJson)
                        profileName = profile.name
                        profileImageUrl = profile.imageUrl
                    } catch (e: Exception) {
                        // Ignore parse errors
                    }
                }

                _username.value = profileName ?: userId
                _userImageUrl.value = profileImageUrl

settingsRepository.saveUserProfile(userId, profileName, profileImageUrl)
            } catch (e: Exception) {
            // Ignore errors
            }
        }
    }

    private fun parseErrorMessage(json: String): String? {
        return try {
            val obj = org.json.JSONObject(json)
            if (obj.has("error")) {
                val err = obj.getJSONObject("error")
                err.optString("message", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}