package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val playlistId: String,
    val uri: String,
    val name: String,
    val ownerName: String,
    val trackCount: Int,
    val imageUrl: String?,

    val lastUpdated: Long,
)
