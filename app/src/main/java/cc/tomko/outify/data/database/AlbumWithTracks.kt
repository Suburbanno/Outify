package cc.tomko.outify.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class AlbumWithTracks(
    @Embedded val album: AlbumEntity,

    @Relation(
        parentColumn = "albumId",
        entityColumn = "id",
        associateBy = Junction(
            value = AlbumTrackCrossRef::class,
            parentColumn = "albumId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
)
