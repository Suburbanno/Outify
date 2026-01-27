package cc.tomko.outify.data

import cc.tomko.outify.data.native.NativeAlbum

data class Album(
    val id: SpotifyUri,
    val name: String,
) {
    fun NativeAlbum.toAlbum() = Album(
        id = SpotifyUri(id),
        name
    )
}
