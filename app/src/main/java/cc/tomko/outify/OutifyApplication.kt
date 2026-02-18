package cc.tomko.outify

import android.app.Application
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.database.AppDatabase
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
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
            .crossfade(false)
            .memoryCache(
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.10)
                    .weakReferencesEnabled(true)
                    .build()
            )
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25)
                    .build()
            ).build()
    }
}