package cc.tomko.outify.data.database

import androidx.room.Embedded
import androidx.room.Relation
import cc.tomko.outify.data.database.impl.LikedTrackEntity

/**
 * Join for cached liked track position with the TrackEntity
 */
data class LikedTrackWithTrack(
    @Embedded val liked: LikedTrackEntity,
    @Relation(
        parentColumn = "trackUri",
        entityColumn = "trackUri"
    )
    val track: TrackEntity?
)