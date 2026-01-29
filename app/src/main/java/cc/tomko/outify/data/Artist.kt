package cc.tomko.outify.data

import kotlinx.serialization.Serializable

/**
 * Contains information about singular Artist.
 * Data sourced from JSON from FFI.
 */
@Serializable
data class Artist(
    val id: String,
    val uri: String,
    val name: String,
    val popularity: Int,
)
