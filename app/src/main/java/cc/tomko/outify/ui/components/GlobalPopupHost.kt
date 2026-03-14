package cc.tomko.outify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.data.Track
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.components.bottomsheet.TrackInfoBottomSheet
import cc.tomko.outify.ui.components.navigation.Route

@Composable
fun GlobalPopupHost(
    backStack: NavBackStack<NavKey>,
    addToQueue: (Track) -> Unit,
    startRadio: (Track) -> Unit,
    toggleLike: (Track) -> Unit,
) {
    val track by GlobalPopupController.popup.collectAsState()

    track?.let {
        TrackInfoBottomSheet(
            track = it,
            onDismiss = { GlobalPopupController.dismiss() },
            onArtworkClick = {
                backStack.add(Route.AlbumScreenFromTrackUri(it.uri))
            },
            onArtistClick = { artist ->
                backStack.add(Route.ArtistScreen(artist.uri))
            },
            onOpenAlbum = {
                backStack.add(Route.AlbumScreenFromTrackUri(it.uri))
            },
            onOpenArtist = {
                backStack.add(Route.ArtistScreen(it.artists.first().uri))
            },
            onAddToQueue = { addToQueue(it) },
            onSaveToPlaylist = {},
            onToggleLike = { toggleLike(it) },
            onStartRadio = { startRadio(it) }
        )
    }
}
