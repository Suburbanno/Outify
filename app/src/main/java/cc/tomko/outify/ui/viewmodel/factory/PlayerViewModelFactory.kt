package cc.tomko.outify.ui.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.viewmodel.player.PlayerViewModel

class PlayerViewModelFactory(
    private val playbackStateHolder: PlaybackStateHolder
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(playbackStateHolder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
