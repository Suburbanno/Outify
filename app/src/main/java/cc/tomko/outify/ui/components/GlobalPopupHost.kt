package cc.tomko.outify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.components.bottomsheet.TrackInfoBottomSheet
import cc.tomko.outify.ui.components.navigation.Route
import kotlinx.coroutines.launch

@Composable
fun GlobalPopupHost(
    backStack: NavBackStack<NavKey>,
    addToQueue: (Track) -> Unit,
    startRadio: (Track) -> Unit,
    toggleLike: (Track) -> Unit,
) {
    val data by GlobalPopupController.popup.collectAsState()
    val scope = rememberCoroutineScope()

    data?.let { data ->
        data.track.let {
            TrackInfoBottomSheet(
                track = it,
                likedTrackIndex = data.likedTrackIndex,
                onDismiss = { GlobalPopupController.dismiss() },
                onArtworkClick = {
                    backStack.add(Route.TrackScreen(it.uri))
                    data.action?.invoke()
                },
                onArtistClick = { artist ->
                    backStack.add(Route.ArtistScreen(artist.uri))
                    data.action?.invoke()
                },
                onOpenAlbum = {
                    backStack.add(Route.TrackScreen(it.uri))
                    data.action?.invoke()
                },
                onOpenArtist = {
                    backStack.add(Route.ArtistScreen(it.artists.first().uri))
                    data.action?.invoke()
                },
                onAddToQueue = { addToQueue(it) },
                onSaveToPlaylist = {},
                onToggleLike = { toggleLike(it) },
                onStartRadio = { startRadio(it) },
                onScrollToLiked = {
                    scope.launch {
                        backStack.add(Route.LikedScreen(scrollToIndex = data.likedTrackIndex ?: -1))
                        GlobalPopupController.dismiss()
                    }
                }
            )
        }
    }
}
