package cc.tomko.outify.data

import kotlinx.serialization.Serializable

/**
 * Contains information about single Track.
 * Data sourced from JSON from FFI
 */
@Serializable
data class Track(
    val id: String,
    val uri: String,
    val name: String,
    val album: Album? = null,
    val artists: List<Artist> = emptyList(),
    val popularity: Int = 0,
    val duration: Long = 0,
    val explicit: Boolean = false,
)