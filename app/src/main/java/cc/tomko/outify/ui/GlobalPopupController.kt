package cc.tomko.outify.ui

import cc.tomko.outify.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

object GlobalPopupController {
    private val _popups = MutableStateFlow<List<PopupSpec>>(emptyList())
    val popups: StateFlow<List<PopupSpec>> = _popups

    fun show(popup: PopupSpec) {
        _popups.value = _popups.value + popup
    }

    fun dismiss(popupId: String) {
        _popups.value = _popups.value.filterNot { it.id == popupId }
    }

    fun dismissAll() {
        _popups.value = emptyList()
    }

    fun dismiss() {
        _popups.value = _popups.value.dropLast(1)
    }
}

sealed class PopupSpec(
    open val id: String = UUID.randomUUID().toString(),
) {
    data class TrackInfo(
        val track: Track,
        val action: (() -> Unit)? = null,
        val likedTrackIndex: Int? = null,
        override val id: String = UUID.randomUUID().toString(),
    ) : PopupSpec(id)

    data class AuthResult(
        val isSuccess: Boolean,
        val message: String = if (isSuccess) "Login successful!" else "Login failed",
        val errorDetails: String? = null,
        val onDismiss: (() -> Unit)? = null,
        override val id: String = UUID.randomUUID().toString(),
    ) : PopupSpec(id)

    data class AddToPlaylist(
        val track: Track,
        override val id: String = UUID.randomUUID().toString(),
    ) : PopupSpec(id)
}