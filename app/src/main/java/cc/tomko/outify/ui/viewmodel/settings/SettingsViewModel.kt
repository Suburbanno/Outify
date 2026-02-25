package cc.tomko.outify.ui.viewmodel.settings

import androidx.lifecycle.ViewModel
import cc.tomko.outify.core.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val session: Session,
) : ViewModel() {

    fun resetSession(){
        session.shutdown()
    }
}