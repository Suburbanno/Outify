package cc.tomko.outify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.PopupSpec
import cc.tomko.outify.ui.components.bottomsheet.AddToPlaylistBottomSheet
import cc.tomko.outify.ui.components.bottomsheet.AuthResultBottomSheet
import cc.tomko.outify.ui.components.bottomsheet.TrackInfoBottomSheet
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.viewmodel.bottomsheet.AddToPlaylistViewModel
import kotlinx.coroutines.launch

@Composable
fun GlobalPopupHost(
    backStack: NavBackStack<NavKey>,
    addToQueue: (Track) -> Unit,
    startRadio: (Track) -> Unit,
    toggleLike: (Track) -> Unit,

    addToPlaylistViewModel: AddToPlaylistViewModel,
) {
    val popups by GlobalPopupController.popups.collectAsState()
    val scope = rememberCoroutineScope()

    popups.forEach { popup ->
        when (popup) {
            is PopupSpec.TrackInfo -> {
                TrackInfoBottomSheet(
                    track = popup.track,
                    likedTrackIndex = popup.likedTrackIndex,
                    onDismiss = { GlobalPopupController.dismiss(popup.id) },
                    onArtworkClick = {
                        backStack.add(Route.TrackScreen(popup.track.uri))
                        popup.action?.invoke()
                    },
                    onArtistClick = { artist ->
                        backStack.add(Route.ArtistScreen(artist.uri))
                        popup.action?.invoke()
                    },
                    onOpenAlbum = {
                        backStack.add(Route.TrackScreen(popup.track.uri))
                        popup.action?.invoke()
                    },
                    onOpenArtist = {
                        backStack.add(Route.ArtistScreen(popup.track.artists.first().uri))
                        popup.action?.invoke()
                    },
                    onAddToQueue = { addToQueue(popup.track) },
                    onSaveToPlaylist = {},
                    onToggleLike = { toggleLike(popup.track) },
                    onStartRadio = { startRadio(popup.track) },
                    onScrollToLiked = {
                        scope.launch {
                            backStack.add(Route.LikedScreen(scrollToIndex = popup.likedTrackIndex ?: -1))
                            GlobalPopupController.dismiss(popup.id)
                        }
                    }
                )
            }

            is PopupSpec.AuthResult -> {
                AuthResultBottomSheet(
                    isSuccess = popup.isSuccess,
                    message = popup.message,
                    errorDetails = popup.errorDetails,
                    onDismiss = {
                        GlobalPopupController.dismiss(popup.id)
                        popup.onDismiss?.invoke()
                    },
                )
            }

            is PopupSpec.AddToPlaylist -> {
                AddToPlaylistBottomSheet(
                    viewModel = addToPlaylistViewModel,
                    tracks = popup.tracks,
                    onDismiss = {
                        GlobalPopupController.dismiss(popup.id)
                    }
                )
            }
        }
    }
}
