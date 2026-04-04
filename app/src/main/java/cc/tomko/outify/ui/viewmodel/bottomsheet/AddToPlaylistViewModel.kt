package cc.tomko.outify.ui.viewmodel.bottomsheet

import androidx.lifecycle.ViewModel
import cc.tomko.outify.data.dao.PlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    val playlistDao: PlaylistDao,
) : ViewModel() {
//    val ownedPlaylists = playlistDao.getPlaylistsWithItems()
}