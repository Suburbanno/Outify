package cc.tomko.outify.data

import androidx.compose.runtime.Immutable
import cc.tomko.outify.data.database.ArtistEntity
import cc.tomko.outify.data.database.TrackArtistEntity
import cc.tomko.outify.data.database.TrackEntity
import cc.tomko.outify.utils.canonicalIdFromUri
import kotlinx.serialization.Serializable

/**
 * Contains information about single Track.
 * Data sourced from JSON from FFI
 */
@Serializable
@Immutable
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

fun Track.toEntities(now: Long): Triple<TrackEntity, List<ArtistEntity>, List<TrackArtistEntity>> {
    val canonicalId = canonicalIdFromUri(id.ifBlank { uri })

    val trackEntity = TrackEntity(
        id = canonicalId,
        trackUri = uri,
        name = name,
        albumId = album?.id,
        albumName = album?.name,
        durationMs = duration,
        explicit = explicit,
        popularity = popularity,
        isLibraryItem = true,
        lastAccessed = now,
        lastUpdated = now
    )

    val artistEntities = artists.map {
        ArtistEntity(
            artistId = canonicalIdFromUri(it.id.ifBlank { it.uri }),
            name = it.name,
            imageUrl = "", // TODO: Add the image urls
            lastUpdated = now,
            uri = "spotify:artist:" + it.id,
            popularity = popularity
        )
    }

    val joins = artists.mapIndexed { index, artist ->
        TrackArtistEntity(
            trackId = canonicalId,
            artistId = artist.id,
            position = index
        )
    }

    return Triple(trackEntity, artistEntities, joins)
}
