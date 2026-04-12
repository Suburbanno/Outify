package cc.tomko.outify

import android.app.Application
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.ui.viewmodel.detail.DetailViewModelStore
import cc.tomko.outify.ui.viewmodel.detail.setDetailViewModelStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

const val ALBUM_COVER_URL: String = "https://i.scdn.co/image/"

@HiltAndroidApp
class OutifyApplication : Application() {
    lateinit var database: AppDatabase
        private set

    @Inject
    lateinit var spircController: SpircController

    @Inject
    lateinit var detailViewModelStore: DetailViewModelStore

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        setDetailViewModelStore(detailViewModelStore)

        System.loadLibrary("librespot_ffi")
        LibrespotFfi.libInit(applicationContext)

        spircController.start()

//        AeadConfig.register()
    }
}