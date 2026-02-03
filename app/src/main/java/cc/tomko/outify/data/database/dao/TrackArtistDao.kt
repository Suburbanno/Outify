package cc.tomko.outify.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import cc.tomko.outify.data.database.TrackArtistEntity

@Dao
interface TrackArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(joins: List<TrackArtistEntity>)
}