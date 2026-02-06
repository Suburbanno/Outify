package cc.tomko.outify.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import cc.tomko.outify.data.Track
import kotlinx.serialization.Serializable
import java.io.Serial

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
    data class AlbumScreen(val albumUri: String): Route, NavKey

    @Serializable
    data class AlbumScreenFromTrack(val track: Track): Route, NavKey
}