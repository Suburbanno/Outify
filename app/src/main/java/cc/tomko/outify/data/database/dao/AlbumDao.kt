package cc.tomko.outify.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cc.tomko.outify.data.database.AlbumEntity
import cc.tomko.outify.data.database.AlbumWithArtists

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums WHERE albumId = :albumId")
    suspend fun getAlbumWithArtists(albumId: String): AlbumWithArtists?

    @Query("SELECT * FROM albums WHERE albumId IN (:albumIds)")
    suspend fun getAlbumsWithArtists(albumIds: List<String>): List<AlbumWithArtists>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<AlbumEntity>)
}