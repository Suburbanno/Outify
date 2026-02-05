package cc.tomko.outify.ui.model.player

/**
 * Holds the UI state of the player
 */
data class PlayerUIState(
    val title: String = "",
    val artist: String = "",
    val albumArt: String? = null,
    val isPlaying: Boolean = false,
    var isExplicit: Boolean = false,

    /**
     * The total track length
     */
    val totalLengthMs: Long = 0L,
    /**
     * Elapsed track time
     */
    val positionMs: Long = 0L,
    /**
     * When did we last get positionMs from Spotify
     */
    val lastUpdateTime: Long = 0L,
)
