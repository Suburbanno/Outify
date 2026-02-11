package cc.tomko.outify.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import cc.tomko.outify.data.database.dao.AlbumArtistDao
import cc.tomko.outify.data.database.dao.AlbumDao
import cc.tomko.outify.data.database.dao.AlbumTrackDao
import cc.tomko.outify.data.database.dao.ArtistDao
import cc.tomko.outify.data.database.dao.TrackArtistDao
import cc.tomko.outify.data.database.dao.TrackDao

@Database(
    entities = [
        TrackEntity::class,
        ArtistEntity::class,
        TrackArtistEntity::class,
        AlbumEntity::class,
        AlbumTrackCrossRef::class,
        AlbumArtistEntity::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun artistDao(): ArtistDao
    abstract fun trackArtistDao(): TrackArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun albumArtistDao(): AlbumArtistDao
    abstract fun albumTrackDao(): AlbumTrackDao

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