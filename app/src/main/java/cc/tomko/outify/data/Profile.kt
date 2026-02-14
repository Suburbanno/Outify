package cc.tomko.outify.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Holds the User profile data
 */
@Serializable
data class Profile(
    val uri: String,
    val name: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("followers_count")
    val followersCount: Int,
    @SerialName("following_count")
    val followingCount: Int,
    @SerialName("has_spotify_name")
    val hasSpotifyName: Boolean,
    @SerialName("has_spotify_image")
    val hasSpotifyImage: Boolean,
    val color: Int,
    @SerialName("allows_follows")
    val allowsFollows: Boolean,
)
