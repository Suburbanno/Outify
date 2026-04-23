package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.repository.PlaybackSettings
import cc.tomko.outify.data.repository.SettingsRepository
import cc.tomko.outify.playback.model.Bitrate
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlaybackSettingViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    val spirc: SpircController
) : ViewModel() {
    private val _needsRestart = MutableStateFlow(false)
    val needsRestart: StateFlow<Boolean> = _needsRestart

    val settings: Flow<PlaybackSettings> =
        settingsRepository.playbackSettings

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            _needsRestart.value = true
            settingsRepository.setGaplessPlayback(enabled)
        }
    }

    fun setNormalizeAudio(enabled: Boolean) {
        viewModelScope.launch {
            _needsRestart.value = true
            settingsRepository.setNormalizePlayback(enabled)
        }
    }

    fun setKeepAlive(enabled: Boolean) {
        viewModelScope.launch {
            _needsRestart.value = true
            settingsRepository.setKeepalive(enabled)
        }
    }

    fun setBitrate(bitrate: Bitrate) {
        viewModelScope.launch {
            _needsRestart.value = true
            settingsRepository.setBitrate(bitrate)
        }
    }

    fun restartSpirc(){
        viewModelScope.launch {
            spirc.restart()
            _needsRestart.value = false
        }
    }
}
