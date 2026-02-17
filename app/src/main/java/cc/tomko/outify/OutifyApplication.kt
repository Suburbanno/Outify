package cc.tomko.outify

import android.app.Application
import android.util.Log
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.SessionCallback
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.metadata.Metadata
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
import kotlinx.coroutines.Runnable
import javax.inject.Inject

const val ALBUM_COVER_URL: String = "https://i.scdn.co/image/"

@HiltAndroidApp
class OutifyApplication : Application() {
    lateinit var database: AppDatabase
        private set

    lateinit var imageLoader: ImageLoader
        private set

    @Inject
    lateinit var spircController: SpircController

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        System.loadLibrary("librespot_ffi")
        LibrespotFfi.libInit(applicationContext)

        spircController.start()

//        AeadConfig.register()

        imageLoader = ImageLoader.Builder(this)
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25)
                    .build()
            ).build()
    }
}