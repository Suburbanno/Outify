package cc.tomko.outify

import android.app.Application
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.playback.AudioManager
import cc.tomko.outify.playback.AudioPlayer
import cc.tomko.outify.playback.PlaybackManager
import cc.tomko.outify.ui.repository.LibraryRepository
import cc.tomko.outify.ui.repository.SearchRepository
import cc.tomko.outify.ui.repository.TrackRepository
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade

class OutifyApplication : Application() {
    lateinit var database: AppDatabase
        private set

    lateinit var audioPlayer: AudioPlayer
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

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        System.loadLibrary("librespot_ffi")
        LibrespotFfi.libInit(applicationContext)

        session = Session()
        session.initializeSession()

        audioPlayer = AudioPlayer(this)
        audioManager = AudioManager(audioPlayer)

        playbackManager = PlaybackManager()
        authManager = AuthManager()
        spirc = Spirc()


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
            albumArtistDao = database.albumArtistDao()
        )

        libraryRepository = LibraryRepository(metadata)
        searchRepository = SearchRepository()
    }

    companion object {
        const val ALBUM_COVER_URL: String = "https://i.scdn.co/image/"

        lateinit var audioManager: AudioManager
        lateinit var session: Session
        lateinit var playbackManager: PlaybackManager
        lateinit var authManager: AuthManager
        lateinit var spirc: Spirc
    }
}