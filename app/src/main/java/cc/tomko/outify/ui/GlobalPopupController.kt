package cc.tomko.outify.ui

import cc.tomko.outify.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object GlobalPopupController {
    private val _popup = MutableStateFlow<TrackPopup?>(null)
    val popup: StateFlow<TrackPopup?> = _popup

    fun showTrackPopup(track: Track, action: (() -> Unit)? = null) {
        _popup.value = TrackPopup(track, action)
    }

    fun dismiss() {
        _popup.value = null
    }
}

data class TrackPopup(
    val track: Track,
    val action: (() -> Unit)? = null,
)