package cc.tomko.outify

import android.app.Application
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.core.spirc.SpircWrapper
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.playback.AudioEngine
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.Player
import cc.tomko.outify.ui.repository.LibraryRepository
import cc.tomko.outify.ui.repository.SearchRepository
import cc.tomko.outify.ui.repository.TrackRepository
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

const val ALBUM_COVER_URL: String = "https://i.scdn.co/image/"

@HiltAndroidApp
class OutifyApplication : Application() {

    @UnstableApi
    lateinit var player: Player
        private set

    lateinit var database: AppDatabase
        private set
    lateinit var trackRepository: TrackRepository
        private set
    lateinit var libraryRepository: LibraryRepository
        private set
    lateinit var searchRepository: SearchRepository
        private set

    lateinit var imageLoader: ImageLoader
        private set
    lateinit var metadata: Metadata
        private set

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        System.loadLibrary("librespot_ffi")
        LibrespotFfi.libInit(applicationContext)

        session = Session()
        session.initializeSession()

        authManager = AuthManager()
        spirc = SpircWrapper(this)

        player = Player(this, stateHolder = playbackStateHolder)

//        AeadConfig.register()
        initializeRepositories()

        imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25)
                    .build()
            ).build()
    }

    private fun initializeRepositories(){
        trackRepository = TrackRepository(database.trackDao())
        metadata = Metadata(
            db = database,
            trackRepo = trackRepository,
            trackDao = database.trackDao(),
            artistDao = database.artistDao(),
            trackArtistDao = database.trackArtistDao(),
            albumDao = database.albumDao(),
            albumArtistDao = database.albumArtistDao(),
            albumTrackDao = database.albumTrackDao(),
        )

        libraryRepository = LibraryRepository(metadata)
        searchRepository = SearchRepository()
    }

    companion object {
        lateinit var session: Session
        lateinit var authManager: AuthManager
        lateinit var spirc: SpircWrapper
        var playbackStateHolder = PlaybackStateHolder()
    }
}