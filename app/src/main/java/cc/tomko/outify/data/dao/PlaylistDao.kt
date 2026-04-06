package cc.tomko.outify.data.dao
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cc.tomko.outify.data.database.PlaylistEntity
import cc.tomko.outify.data.database.playlist.PlaylistItemEntity
import cc.tomko.outify.data.database.playlist.PlaylistWithItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Dao
@Singleton
interface PlaylistDao {
    data class CoverUris(
        @ColumnInfo(name = "albumCoverBaseUrl") val baseUrl: String,
        @ColumnInfo(name = "smallCoverUri") val small: String?,
        @ColumnInfo(name = "mediumCoverUri") val medium: String?,
        @ColumnInfo(name = "largeCoverUri") val large: String?
    )

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistWithItems(id: String): PlaylistWithItems?

    @Transaction
    @Query("SELECT * FROM playlists WHERE id IN (:ids)")
    suspend fun getPlaylistsWithItems(ids: List<String>): List<PlaylistWithItems>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id IN (:ids)")
    fun getPlaylistsWithItemsFlow(ids: List<String>): Flow<List<PlaylistWithItems>>
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getPlaylistsWithItemsFlow(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistWithItemsFlow(id: String): Flow<PlaylistWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(entity: PlaylistEntity)

    @Query("UPDATE playlists SET cachedArtworkUrl = :artworkUrl WHERE id = :playlistId")
    suspend fun updateCachedArtworkUrl(playlistId: String, artworkUrl: String?)

    @Query("""
        SELECT a.albumCoverBaseUrl, a.smallCoverUri, a.mediumCoverUri, a.largeCoverUri 
        FROM albums a
        INNER JOIN album_tracks at ON a.albumId = at.albumId
        INNER JOIN tracks t ON at.trackId = t.id
        WHERE t.trackUri = :trackUri
        LIMIT 1
    """)
    suspend fun getCoverUris(trackUri: String): CoverUris?

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteItems(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaylistItemEntity>)
}
