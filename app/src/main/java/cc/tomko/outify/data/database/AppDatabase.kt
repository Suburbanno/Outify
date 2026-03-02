package cc.tomko.outify.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cc.tomko.outify.data.database.album.AlbumArtistEntity
import cc.tomko.outify.data.database.album.AlbumTrackCrossRef
import cc.tomko.outify.data.database.dao.AlbumArtistDao
import cc.tomko.outify.data.database.dao.AlbumDao
import cc.tomko.outify.data.database.dao.AlbumTrackDao
import cc.tomko.outify.data.database.dao.ArtistDao
import cc.tomko.outify.data.database.dao.LikedDao
import cc.tomko.outify.data.database.dao.PlaylistDao
import cc.tomko.outify.data.database.dao.TrackArtistDao
import cc.tomko.outify.data.database.dao.TrackDao
import cc.tomko.outify.data.database.impl.LikedTrackEntity
import cc.tomko.outify.data.database.impl.PlaylistTrackEntity
import cc.tomko.outify.data.database.playlist.PlaylistDiffEntity
import cc.tomko.outify.data.database.playlist.PlaylistItemEntity

@Database(
    entities = [
        TrackEntity::class,
        ArtistEntity::class,
        TrackArtistEntity::class,
        AlbumEntity::class,
        AlbumTrackCrossRef::class,
        AlbumArtistEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlaylistDiffEntity::class,
        PlaylistTrackEntity::class,
        LikedTrackEntity::class,
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun artistDao(): ArtistDao
    abstract fun trackArtistDao(): TrackArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun albumArtistDao(): AlbumArtistDao
    abstract fun albumTrackDao(): AlbumTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun likedDao(): LikedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "outify_database"
                )
                    .fallbackToDestructiveMigration(true) // IMPORTANT TODO: REMOVE IN PROD
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}