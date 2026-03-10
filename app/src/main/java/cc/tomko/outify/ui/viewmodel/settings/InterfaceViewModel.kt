package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ui.repository.InterfaceSettings
import cc.tomko.outify.ui.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: Flow<InterfaceSettings> =
        settingsRepository.interfaceSettings
}