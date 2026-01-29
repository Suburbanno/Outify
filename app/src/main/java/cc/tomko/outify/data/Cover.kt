package cc.tomko.outify.data

import kotlinx.serialization.Serializable

/**
 * Contains information about the cover art
 */
@Serializable
data class Cover(
    /**
     * Just the ID of the image
     */
    val uri: String,
    val width: Int,
    val height: Int
)
