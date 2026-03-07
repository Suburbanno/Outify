package cc.tomko.outify.ui

import cc.tomko.outify.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object GlobalPopupController {
    private val _popup = MutableStateFlow<Track?>(null)
    val popup: StateFlow<Track?> = _popup

    fun showTrackPopup(track: Track) {
        _popup.value = track
    }

    fun dismiss() {
        _popup.value = null
    }
}