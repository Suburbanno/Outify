package cc.tomko.outify.data.database.dao
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
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistWithItemsFlow(id: String): Flow<PlaylistWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(entity: PlaylistEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteItems(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaylistItemEntity>)
}