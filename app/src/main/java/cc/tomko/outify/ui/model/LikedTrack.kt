package cc.tomko.outify.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class LikedTrack(
    val uri: String,
    val imageUrl: String,
    val title: String,
    val artist: String
)