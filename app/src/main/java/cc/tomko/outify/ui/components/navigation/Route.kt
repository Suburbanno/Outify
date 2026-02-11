package cc.tomko.outify.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.data.Track
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

    /**
     * When we know only the Track uri and want to open the Album Screen
     */
    @Serializable
    data class AlbumScreen(val trackUri: String): Route, NavKey

    /**
     * When we know the Track and want to open Album Screen
     */
    @Serializable
    data class AlbumScreenFromTrack(val track: Track): Route, NavKey

    @Serializable
    data class ArtistScreen(val artistUri: String): Route, NavKey
}