package cc.tomko.outify.data

import kotlinx.serialization.Serializable

/**
 * Contains information about Album.
 * Fields sourced from JSON from FFI
 */
@Serializable
data class Album(
    val id: String,
    val uri: String,
    val name: String,
    val artists: List<Artist> = emptyList(),
    val popularity: Int = 0,
    val covers: List<Cover> = emptyList(),
)
