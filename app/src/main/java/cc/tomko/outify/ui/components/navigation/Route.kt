package cc.tomko.outify.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data object HomeScreen: Route, NavKey

    @Serializable
    data object SearchScreen: Route, NavKey

    @Serializable
    data object PlayerScreen: Route, NavKey

    @Serializable
    data object LikedScreen: Route, NavKey

    @Serializable
    data object LibraryScreen: Route, NavKey

    @Serializable
    data class PlaylistScreen(val playlistUri: String): Route, NavKey

    /**
     * When we know only the Track uri and want to open the Album Screen
     */
    @Serializable
    data class AlbumScreenFromAlbumUri(val albumUri: String): Route, NavKey

    @Serializable
    data class AlbumScreenFromTrackUri(val trackUri: String): Route, NavKey

    @Serializable
    data class ArtistScreen(val artistUri: String): Route, NavKey

    // Settings
    @Serializable
    data object SettingsScreen: Route, NavKey

    @Serializable
    data object InterfaceSettings: Route, NavKey

    @Serializable
    data object GestureSettings: Route, NavKey
}