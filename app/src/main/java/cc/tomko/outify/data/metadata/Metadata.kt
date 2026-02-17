package cc.tomko.outify.data.metadata

import android.util.Log
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Album
import cc.tomko.outify.data.Artist
import cc.tomko.outify.data.Playlist
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.dao.AlbumArtistDao
import cc.tomko.outify.data.database.dao.AlbumDao
import cc.tomko.outify.data.database.dao.AlbumTrackDao
import cc.tomko.outify.data.database.dao.ArtistDao
import cc.tomko.outify.data.database.dao.PlaylistDao
import cc.tomko.outify.data.database.dao.TrackArtistDao
import cc.tomko.outify.data.database.dao.TrackDao
import cc.tomko.outify.ui.repository.TrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class Metadata(
    db: AppDatabase,
    trackRepo: TrackRepository,
    trackDao: TrackDao,
    artistDao: ArtistDao,
    playlistDao: PlaylistDao,
    trackArtistDao: TrackArtistDao,
    albumDao: AlbumDao,
    albumArtistDao: AlbumArtistDao,
    albumTrackDao: AlbumTrackDao,
    concurrency: Int = 10
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val trackMetadataHelper = TrackMetadataHelper(db, trackRepo, trackDao, artistDao, trackArtistDao, albumDao, albumArtistDao, concurrency, this, json)
    private val albumMetadataHelper = AlbumMetadataHelper(db, albumDao, albumArtistDao, albumTrackDao, concurrency, this, json)
    val playlistMetadataHelper = PlaylistMetadataHelper(db, playlistDao, concurrency, this, json)

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
            val raw = getNativeMetadata(uri)
            return json.decodeFromString<Artist>(raw)
        } catch (e: Exception) {
            Log.e("Metadata", "fetchAlbums: failed for $uri", e)
            return null
        }
    }

    suspend fun getPlaylistUris(): List<String> {
        try {
            val uris = OutifyApplication.session.spClient.getRootlist()
            return uris.toList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun getPlaylistMetadata(uri: String): Playlist? {
        return playlistMetadataHelper.getPlaylistMetadata(uri)
    }

    fun observePlaylist(uri: String) =
        playlistMetadataHelper.observePlaylist(uri)
    fun observePlaylists(uris: List<String>) =
        playlistMetadataHelper.observePlaylists(uris)

    /**
     * Native method to get JSON Metadata using URI
     */
    external fun getNativeMetadata(uri: String): String
}