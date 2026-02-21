package cc.tomko.outify

import android.content.Context
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.dao.AlbumArtistDao
import cc.tomko.outify.data.database.dao.AlbumDao
import cc.tomko.outify.data.database.dao.AlbumTrackDao
import cc.tomko.outify.data.database.dao.ArtistDao
import cc.tomko.outify.data.database.dao.LikedDao
import cc.tomko.outify.data.database.dao.PlaylistDao
import cc.tomko.outify.data.database.dao.TrackArtistDao
import cc.tomko.outify.data.database.dao.TrackDao
import cc.tomko.outify.data.metadata.Metadata
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return (context.applicationContext as OutifyApplication).database
    }

    @Provides
    @Named("metadataConcurrency")
    fun provideMetadataConcurrency(): Int = 10

    @Provides
    @Singleton
    fun provideJson(): Json{
        return json
    }

    @Provides
    @Singleton
    fun provideClock(): Clock {
        return Clock.systemUTC()
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: AppDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    @Singleton
    fun provideArtistDao(database: AppDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    @Singleton
    fun provideTrackArtistDao(database: AppDatabase): TrackArtistDao {
        return database.trackArtistDao()
    }

    @Provides
    @Singleton
    fun provideAlbumDao(database: AppDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    @Singleton
    fun provideAlbumArtistDao(database: AppDatabase): AlbumArtistDao {
        return database.albumArtistDao()
    }

    @Provides
    @Singleton
    fun provideAlbumTrackDao(database: AppDatabase): AlbumTrackDao {
        return database.albumTrackDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun provideLikedDao(database: AppDatabase): LikedDao {
        return database.likedDao()
    }
}