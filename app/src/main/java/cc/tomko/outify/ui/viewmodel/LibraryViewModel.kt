package cc.tomko.outify.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.ui.model.LikedTrack
import cc.tomko.outify.ui.repository.LibraryRepository
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository
): ViewModel() {
    var tracks by mutableStateOf<List<LikedTrack>>(emptyList())
        private set

    init  {
        viewModelScope.launch {
            tracks = repository.getLikedTracks()
        }
    }
}