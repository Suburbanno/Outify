package cc.tomko.outify.data

import cc.tomko.outify.data.native.NativeArtist

data class Artist(
    val id: SpotifyUri,
    val name: String,
    val popularity: Int,
) {
    fun NativeArtist.toArtist(): Artist = Artist(
        id = SpotifyUri(id),
        name,
        popularity
    )
}
