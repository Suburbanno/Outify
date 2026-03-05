package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.data.setting.GestureAction
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.Side
import cc.tomko.outify.ui.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureSettingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val swipeEnabled = settingsRepository.interfaceSettings
        .map { it.swipeGesturesEnabled }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val gestures = settingsRepository.interfaceSettings
        .map { it.gestureSettings }
        .stateIn(viewModelScope, SharingStarted.Lazily, SettingsRepository.Gesture.defaultGestureList())

    // --- Mutations ---

    fun setGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGesturesEnabled(enabled)
        }
    }

    fun saveGestures(list: List<GestureSetting>) {
        viewModelScope.launch {
            settingsRepository.saveGestures(list)
        }
    }

    fun addGesture(gesture: GestureSetting = GestureSetting(
        action = GestureAction.NONE,
        side = Side.End,
        enabled = true,
        thresholdFraction = 0.25f,
        backgroundHex = null
    )) {
        viewModelScope.launch {
            val current = gestures.value
            val next = current.toMutableList().apply { add(gesture) }
            settingsRepository.saveGestures(next)
        }
    }

    fun removeGesture(index: Int) {
        viewModelScope.launch {
            val current = gestures.value.toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                settingsRepository.saveGestures(current)
            }
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val current = gestures.value.toMutableList()
            if (index in current.indices) {
                val item = current.removeAt(index)
                val insertIndex = index - 1
                current.add(insertIndex, item)
                settingsRepository.saveGestures(current)
            }
        }
    }

    fun moveDown(index: Int) {
        viewModelScope.launch {
            val current = gestures.value.toMutableList()
            val lastIndex = current.size - 1
            if (index in current.indices && index < lastIndex) {
                val item = current.removeAt(index)
                val insertIndex = index + 1
                current.add(insertIndex, item)
                settingsRepository.saveGestures(current)
            }
        }
    }

    fun updateGestureAt(index: Int, updated: GestureSetting) {
        viewModelScope.launch {
            val current = gestures.value.toMutableList()
            if (index in current.indices) {
                current[index] = updated
                settingsRepository.saveGestures(current)
            }
        }
    }
}