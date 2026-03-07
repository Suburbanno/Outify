package cc.tomko.outify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.components.bottomsheet.TrackInfoBottomSheet

@Composable
fun GlobalPopupHost() {
    val track by GlobalPopupController.popup.collectAsState()

    track?.let {
        TrackInfoBottomSheet(
            track = it,
            onDismiss = { GlobalPopupController.dismiss() },
            onArtworkClick = {},
            onArtistClick = {},
            onOpenAlbum = {},
            onAddToQueue = {},
            onSaveToPlaylist = {},
            onToggleLike = {},
            onStartRadio = {}
        )
    }
}
