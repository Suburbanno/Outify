package cc.tomko.outify.data.native

/**
 * Represents the native track from librespot
 */
data class NativeTrack(
    val id: String,
    val name: String,
    val duration: Int,
    val popularity: Int,
    val isExplicit: Boolean,
)