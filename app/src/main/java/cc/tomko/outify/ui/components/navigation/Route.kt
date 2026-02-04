package cc.tomko.outify.ui.components.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import java.io.Serial

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data object HomeScreen: Route, NavKey

    @Serializable
    data object PlayerScreen: Route, NavKey

    @Serializable
    data object LikedScreen: Route, NavKey
}