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
    val height: Int,
    val size: Int? = null,
)

fun Int?.toCoverSize(): CoverSize =
    when (this) {
        1 -> CoverSize.SMALL
        0 -> CoverSize.MEDIUM
        2 -> CoverSize.LARGE
        else -> CoverSize.MEDIUM
    }

enum class CoverSize {
    SMALL,
    MEDIUM,
    LARGE
}

fun CoverSize.asInt(): Int =
    when (this) {
        CoverSize.SMALL -> 1
        CoverSize.MEDIUM -> 0
        CoverSize.LARGE -> 2
    }