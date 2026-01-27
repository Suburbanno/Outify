package cc.tomko.outify.data

import cc.tomko.outify.data.native.NativeTrack

/**
 * Holds the data about each track
 */
data class Track(
    val id: SpotifyUri,
    val name: String,
    val album: Album,
    val artists: List<Artist>,
    val duration: Int,
    val popularity: Int,
    val isExplicit: Boolean,
    // TODO: Implement more data

) {
    fun NativeTrack.toTrack(album: Album, artists: List<Artist>): Track = Track(
        id = SpotifyUri(id),
        name,
        album,
        artists,
        duration,
        popularity,
        isExplicit,
    )
}