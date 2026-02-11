package cc.tomko.outify.data

import cc.tomko.outify.ui.screens.library.album.AlbumHeaderUi
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
    /**
     * This list contains just the SpotifyUris of all tracks within the album
     */
    val tracks: List<String> = emptyList(),
    val covers: List<Cover> = emptyList(),
)

fun Album.getCover(size: CoverSize): Cover? {
    return covers.firstOrNull { it.size == size.asInt() }
}