package cc.tomko.outify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.core.model.OutifyUri
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.toOutifyUri
import cc.tomko.outify.ui.GlobalPopupController
import cc.tomko.outify.ui.PopupSpec
import cc.tomko.outify.ui.components.bottomsheet.AddToPlaylistBottomSheet
import cc.tomko.outify.ui.components.bottomsheet.AuthResultBottomSheet
import cc.tomko.outify.ui.components.bottomsheet.PlaybackDevicesBottomSheet
import cc.tomko.outify.ui.components.bottomsheet.PlaylistInfoBottomSheet
import cc.tomko.outify.ui.components.bottomsheet.TrackInfoBottomSheet
import cc.tomko.outify.ui.components.navigation.Route
import cc.tomko.outify.ui.viewmodel.bottomsheet.AddToPlaylistViewModel
import cc.tomko.outify.ui.viewmodel.bottomsheet.PlaybackDevicesViewModel
import kotlinx.coroutines.launch

@Composable
fun GlobalPopupHost(
    backStack: NavBackStack<NavKey>,
    addToQueue: (OutifyUri) -> Unit,
    playNext: (OutifyUri) -> Unit,
    startRadio: (Track) -> Unit,
    openRadio: (Track) -> Unit,
    addToPlaylist: (Track) -> Unit,
    toggleLike: (OutifyUri) -> Unit,

    addToPlaylistViewModel: AddToPlaylistViewModel,
    playbackDevicesViewModel:  PlaybackDevicesViewModel,
) {
    val popups by GlobalPopupController.popups.collectAsState()
    val scope = rememberCoroutineScope()

    popups.forEach { popup ->
        when (popup) {
            is PopupSpec.TrackInfo -> {
                TrackInfoBottomSheet(
                    track = popup.track,
                    likedTrackIndex = popup.likedTrackIndex,
                    isLiked = popup.isLiked,
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
                    onAddToQueue = { addToQueue(popup.track.toOutifyUri()) },
                    onPlayNext = { playNext(popup.track.toOutifyUri()) },
                    onAddToPlaylist = { addToPlaylist(popup.track) },
                    onToggleLike = { toggleLike(popup.track.toOutifyUri()) },
                    onStartRadio = { startRadio(popup.track) },
                    onOpenRadio = { openRadio(popup.track) },
                    onScrollToLiked = {
                        scope.launch {
                            backStack.add(Route.LikedScreen(scrollToIndex = popup.likedTrackIndex ?: -1))
                            GlobalPopupController.dismiss(popup.id)
                        }
                    }
                )
            }

            is PopupSpec.PlaylistInfo -> {
                PlaylistInfoBottomSheet(
                    playlist = popup.playlist,
                    artworkUrl = popup.artworkUrl,
                    onDismiss = { GlobalPopupController.dismiss(popup.id) },
                    onOpenPlaylist = {
                        backStack.add(Route.PlaylistScreen(popup.playlist.uri))
                        GlobalPopupController.dismiss(popup.id)
                    },
                    onOpenCreator = {
                        backStack.add(Route.ProfileScreen(popup.playlist.uri.substringBefore(":").let { "spotify:user:$it" }))
                        GlobalPopupController.dismiss(popup.id)
                    },

                    onAddToQueue = { addToQueue(popup.playlist.toOutifyUri()) },
                    onPlayNext = { playNext(popup.playlist.toOutifyUri()) },
                    onToggleLike = { toggleLike(popup.playlist.toOutifyUri()) },
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

            is PopupSpec.PlaybackDevices -> {
                PlaybackDevicesBottomSheet(
                    viewModel = playbackDevicesViewModel,
                    onDismiss = {
                        GlobalPopupController.dismiss(popup.id)
                    }
                )
            }
        }
    }
}
