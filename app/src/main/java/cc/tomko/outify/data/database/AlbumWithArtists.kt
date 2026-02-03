package cc.tomko.outify.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Cover

data class AlbumWithArtists (
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumn = "albumId",
        entityColumn = "artistId",
        associateBy = Junction(AlbumArtistEntity::class)
    )
    val artists: List<ArtistEntity>
)

fun AlbumWithArtists.toDomain(): Album {
    val domainArtists = artists.map { it.toDomain() }
    return Album(
        id = album.albumId,
        uri = album.uri,
        name = album.name,
        artists = domainArtists,
        popularity = album.popularity,
        covers = listOf(
            Cover(
                uri = album.coverUri ?: "null",
                width = 420,
                height = 420,
            )
        )
    )
}