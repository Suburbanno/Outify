package cc.tomko.outify.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import cc.tomko.outify.data.database.LikedTrackWithTrack
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Dao
@Singleton
interface LikedDao {
    @Transaction
    @Query("""
        SELECT * FROM liked_songs
        ORDER BY addedAt DESC
    """)
    fun observeLikedTracks(): Flow<List<LikedTrackWithTrack>>

    @Query("""
        SELECT EXISTS(SELECT * FROM liked_songs WHERE trackUri = :uri)
    """)
    fun containsTrack(uri: String): Boolean
}