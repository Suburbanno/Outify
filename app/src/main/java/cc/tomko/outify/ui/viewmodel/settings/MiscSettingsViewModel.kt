package cc.tomko.outify.ui.viewmodel.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.repository.LikedRepository
import cc.tomko.outify.services.OAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Progress(val current: Int, val total: Int) : SyncStatus()
    data object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@HiltViewModel
class MiscSettingsViewModel @Inject constructor(
    private val likedRepository: LikedRepository,
    private val spClient: SpClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount: StateFlow<Int> = _likedCount.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        viewModelScope.launch {
            likedRepository.observeCount().collect { count ->
                _likedCount.value = count
            }
        }
        checkAuthState()
    }

    fun checkAuthState() {
        _isAuthenticated.value = spClient.isOAuthAuthenticated()
    }

    fun syncLikedTracks() {
        if (_syncStatus.value is SyncStatus.Syncing || _syncStatus.value is SyncStatus.Progress) return
        if (!spClient.isOAuthAuthenticated()) {
            _syncStatus.value = SyncStatus.Error("Please log in to Spotify account first")
            return
        }

        OAuthService.start(context)
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                val urisSynced = likedRepository.syncLikedTracks(forceSync = true)
                if (urisSynced) {
                    _syncStatus.value = SyncStatus.Success
                } else {
                    _syncStatus.value = SyncStatus.Error("Failed to sync liked tracks")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown error")
            } finally {
                OAuthService.stop(context)
            }

            launch {
                delay(3000)
                if (_syncStatus.value is SyncStatus.Success || _syncStatus.value is SyncStatus.Error) {
                    _syncStatus.value = SyncStatus.Idle
                }
            }
        }
    }

    fun resetStatus() {
        _syncStatus.value = SyncStatus.Idle
    }
}