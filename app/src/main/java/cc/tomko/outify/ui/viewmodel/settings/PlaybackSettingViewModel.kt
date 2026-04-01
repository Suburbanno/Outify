package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ui.repository.PlaybackSettings
import cc.tomko.outify.ui.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@HiltViewModel
class PlaybackSettingViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: Flow<PlaybackSettings> =
        settingsRepository.playbackSettings


    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGaplessPlayback(enabled)
        }
    }

    fun setNormalizeAudio(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNormalizePlayback(enabled)
        }
    }
}
