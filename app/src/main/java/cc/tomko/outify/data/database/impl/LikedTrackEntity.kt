package cc.tomko.outify.data.database.impl

import androidx.room.Entity

@Entity(
    tableName = "liked_songs",
    primaryKeys = ["trackId"]
)
data class LikedTrackEntity(
    val trackId: String,
    val position: Double, // So we can do 1.5
    val addedAt: Long
)

