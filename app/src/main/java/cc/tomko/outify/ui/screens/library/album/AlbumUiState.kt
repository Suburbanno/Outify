package cc.tomko.outify.ui.screens.library.album

import cc.tomko.outify.data.Track

data class AlbumUiState(
    val isLoading: Boolean = true,
    val album: AlbumHeaderUi? = null,
    val tracks: List<Track> = emptyList(),
    val isPlayable: Boolean = true,
    val error: String? = null
)

