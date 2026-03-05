package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.Spirc.SpircWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val session: Session,
    val spirc: SpircWrapper,
) : ViewModel() {
}