package cc.tomko.outify.data.metadata

import android.util.Log
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.Playlist
import cc.tomko.outify.data.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Metadata @Inject constructor(
    private val trackMetadataHelper: TrackMetadataHelper,
    private val albumMetadataHelper: AlbumMetadataHelper,
    private val playlistMetadataHelper: PlaylistMetadataHelper,
    private val nativeMetadata: NativeMetadata,
    private val spClient: SpClient,
    private val json: Json,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTracks(uris: List<String>): Flow<List<Track>> {
        return trackMetadataHelper.observeTracks(uris)
    }

    /**
     * Returns list of Tracks with their metadata
     */
    suspend fun getTrackMetadata(uris: List<String>): List<Track> {
        return trackMetadataHelper.getTrackMetadata(uris)
    }

    /**
     * Returns the Track's album URI
     */
    suspend fun getTrackAlbumId(trackUri: String): String? {
        return trackMetadataHelper.getTrackAlbumId(trackUri)
    }

    /**
     * Returns the cached album with its tracks (ordered URIs).
     *
     * If album missing in DB -> fetch remote, persist, return fetched.
     * If album exists but album_tracks missing -> fetch remote, persist cross-refs, return fetched.
     * If album + album_tracks exist -> fetch remote, compare track lists:
     *      - if different -> persist remote and return it
     *      - if identical  -> return cached immediately
     */
    suspend fun getAlbumMetadata(uri: String): Album? {
        return albumMetadataHelper.getAlbumMetadata(uri)
    }

    suspend fun getArtistMetadata(uri: String): Artist? {
        try {
            val raw = nativeMetadata.getNativeMetadata(uri)
            return json.decodeFromString<Artist>(raw)
        } catch (e: Exception) {
            Log.e("Metadata", "fetchAlbums: failed for $uri", e)
            return null
        }
    }

    suspend fun getPlaylistUris(): List<String> {
        try {
            val uris = spClient.getRootlist()
            return uris.toList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun getLikedUris(): List<String> {
        val jsonUris = spClient.getUserCollection()
        val parsed = json.decodeFromString<List<String>>(jsonUris)
        return parsed
    }

    suspend fun getPlaylistMetadata(uri: String): Playlist? {
        return playlistMetadataHelper.getPlaylistMetadata(uri)
    }

    fun observePlaylist(uri: String) =
        playlistMetadataHelper.observePlaylist(uri)
    fun observePlaylists(uris: List<String>) =
        playlistMetadataHelper.observePlaylists(uris)
}