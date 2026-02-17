package cc.tomko.outify.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cc.tomko.outify.data.database.LikedTrackWithTrack
import cc.tomko.outify.data.database.TrackEntity
import cc.tomko.outify.data.database.TrackWithArtists
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Dao
@Singleton
interface TrackDao {
    /**
     * Inserts singular TrackEntity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    /**
     * Inserts multiple TrackEntities
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(track: List<TrackEntity>)

    @Query(
        """
        SELECT * FROM tracks
        WHERE trackUri IN (:uris)
    """
    )
    suspend fun getTracksByUris(uris: List<String>): List<TrackEntity>

    @Query("""
        SELECT albumId FROM tracks
        WHERE trackUri = :uri
    """)
    suspend fun getAlbumIdForTrack(uri: String): String?

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackUri = :trackUri")
    suspend fun getTrackWithArtists(trackUri: String): TrackWithArtists?

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackUri IN (:trackUris)")
    suspend fun getTracksWithArtists(trackUris: List<String>): List<TrackWithArtists>

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackUri IN (:uris)")
    fun getTracksWithArtistsFlow(uris: List<String>): Flow<List<TrackWithArtists>>

    /**
     * Update the lastAccessed value of TrackEntity
     */
    @Query(
        """
        UPDATE tracks
        SET lastAccessed = :lastAccessed
        WHERE trackUri = :trackUri
    """
    )
    suspend fun updateLastAccessed(trackUri: String, lastAccessed: Long)

    /**
     * Updates the lastAccessed value of multiple TrackEntities
     */
    @Query(
        """
        UPDATE tracks
        SET lastAccessed = :lastAccessed
        WHERE trackUri IN (:trackUris)
    """
    )
    suspend fun updateLastAccessedBatch(trackUris: List<String>, lastAccessed: Long)

    /**
     * Evicts old cached entities
     */
    @Query("""
        DELETE FROM tracks
        WHERE isLibraryItem = 0
        AND lastAccessed < :cutoff
    """)
    suspend fun evictOldCache(cutoff: Long)

    @Query("""
        SELECT EXISTS(SELECT trackUri FROM tracks WHERE trackUri = :uri)
    """)
    suspend fun containsTrackByUri(uri: String): Boolean
}
