package cc.tomko.outify.ui.repository

import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.ui.model.LikedTrack
import kotlinx.serialization.json.Json

class LibraryRepository {
    suspend fun getLikedTracks(): List<LikedTrack> {
        val api = OutifyApplication.session.spClient
        val json = api.getLikedSongs()
        val uris: List<String> = Json.decodeFromString(json)

        return uris.map { uri ->
            LikedTrack(
                uri = uri
            )
        }
    }
}