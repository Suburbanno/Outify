package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Plain album entity with basic info.
 * Use AlbumWithArtists.kt instead.
 */
@Entity(
    tableName = "albums",
)
data class AlbumEntity(
    @PrimaryKey val albumId: String,
    val uri: String,
    val name: String,
    val artistNames: String,
    val coverUri: String?, // Just the hash at the end of the URL
    val popularity: Int,

    val lastUpdated: Long,
)