package cc.tomko.outify.ui

import cc.tomko.outify.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object GlobalPopupController {
    private val _popup = MutableStateFlow<TrackPopup?>(null)
    val popup: StateFlow<TrackPopup?> = _popup

    fun showTrackPopup(track: Track, action: (() -> Unit)? = null, likedTrackIndex: Int? = null) {
        _popup.value = TrackPopup(track, action, likedTrackIndex)
    }

    fun dismiss() {
        _popup.value = null
    }
}

data class TrackPopup(
    val track: Track,
    val action: (() -> Unit)? = null,
    val likedTrackIndex: Int? = null,
)