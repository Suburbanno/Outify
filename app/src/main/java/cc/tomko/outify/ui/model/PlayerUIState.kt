package cc.tomko.outify.ui.model

/**
 * Holds the UI state of the player
 */
data class PlayerUIState(
    val title: String = "",
    val artist: String = "",
    val albumArt: String? = null,
    val isPlaying: Boolean = false
)
