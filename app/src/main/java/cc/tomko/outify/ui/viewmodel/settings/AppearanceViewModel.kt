package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ui.repository.InterfaceSettings
import cc.tomko.outify.ui.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: Flow<InterfaceSettings> =
        settingsRepository.interfaceSettings

    fun setMonochromeImages(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeImages(enabled)
        }
    }

    fun setMonochromeAlbums(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeAlbums(enabled)
        }
    }

    fun setMonochromeArtists(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeArtists(enabled)
        }
    }

    fun setMonochromePlaylists(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromePlaylists(enabled)
        }
    }

    fun setMonochromeTracks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeTracks(enabled)
        }
    }

    fun setMonochromePlayer(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromePlayer(enabled)
        }
    }
}
