package cc.tomko.outify.data.database.impl

import androidx.room.Entity

@Entity(primaryKeys = ["trackId"])
data class LibraryTrackEntity(
    val trackId: String,
    val addedAt: Long,
)
