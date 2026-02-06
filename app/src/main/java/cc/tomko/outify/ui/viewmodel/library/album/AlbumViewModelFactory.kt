package cc.tomko.outify.ui.viewmodel.library.album

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AlbumViewModelFactory(
    private val application: Application,
    private val albumUri: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumViewModel(
            application,
            albumUri,
        ) as T
    }
}