package cc.tomko.outify.data.metadata

import android.util.Log
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.Cover
import cc.tomko.outify.data.CoverSize
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

    fun observeAlbums(uris: List<String>): Flow<List<Album>> {
        return albumMetadataHelper.observeAlbums(uris)
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

    suspend fun getAlbumCover(albumId: String, size: CoverSize): Cover? {
        return albumMetadataHelper.getCoverByAlbumId(albumId, size)
    }

    suspend fun getAlbumCoverByTrackId(trackId: String, size: CoverSize): Cover? {
        return albumMetadataHelper.getCoverByTrackId(trackId, size)
    }

    suspend fun getArtistMetadata(uri: String): Artist? {
        try {
            val raw = nativeMetadata.getNativeMetadata(uri)
            NativeErrorHandler.handleErrorJson(raw, "getArtistMetadata:$uri")
            return json.decodeFromString<Artist>(raw)
        } catch (e: Exception) {
            Log.e("Metadata", "getArtistMetadata: failed for $uri", e)
            NativeErrorHandler.handleError(
                NativeError.fromJson("unknown", e.message ?: "Failed to get artist metadata"),
                "getArtistMetadata:$uri"
            )
            return null
        }
    }

    suspend fun getPlaylistUris(): List<String> {
        try {
            val uris = spClient.getRootlist()
            return uris.toList()
        } catch (e: Exception) {
            NativeErrorHandler.handleError(
                NativeError.fromJson("unknown", e.message ?: "Failed to get playlist URIs"),
                "getPlaylistUris"
            )
            return emptyList()
        }
    }

    suspend fun getLikedUris(): List<String> {
        try {
            val jsonUris = spClient.getUserCollection()
            val checked = spClient.checkAndHandleError(jsonUris, "getLikedUris")
            val parsed = json.decodeFromString<List<String>>(checked)
            return parsed
        } catch (e: Exception) {
            NativeErrorHandler.handleError(
                NativeError.fromJson("unknown", e.message ?: "Failed to get liked URIs"),
                "getLikedUris"
            )
            return emptyList()
        }
    }

    suspend fun getPlaylistMetadata(uri: String, allowCached: Boolean): Playlist? {
        return playlistMetadataHelper.getPlaylistMetadata(uri, allowCached)
    }

    fun observePlaylist(uri: String) =
        playlistMetadataHelper.observePlaylist(uri)
    fun observePlaylists(uris: List<String>) =
        playlistMetadataHelper.observePlaylists(uris)
}