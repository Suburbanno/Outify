package cc.tomko.outify.data

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.database.AlbumArtistEntity
import cc.tomko.outify.data.database.AlbumEntity
import cc.tomko.outify.data.database.AlbumWithArtists
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.ArtistEntity
import cc.tomko.outify.data.database.TrackArtistEntity
import cc.tomko.outify.data.database.TrackEntity
import cc.tomko.outify.data.database.TrackWithArtists
import cc.tomko.outify.data.database.dao.AlbumArtistDao
import cc.tomko.outify.data.database.dao.AlbumDao
import cc.tomko.outify.data.database.dao.ArtistDao
import cc.tomko.outify.data.database.dao.TrackArtistDao
import cc.tomko.outify.data.database.dao.TrackDao
import cc.tomko.outify.data.database.toDomain
import cc.tomko.outify.ui.repository.TrackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class Metadata(
    private val db: AppDatabase,
    private val trackRepo: TrackRepository,
    private val trackDao: TrackDao,
    private val artistDao: ArtistDao,
    private val trackArtistDao: TrackArtistDao,
    private val albumDao: AlbumDao,
    private val albumArtistDao: AlbumArtistDao,
    private val spClient: SpClient = OutifyApplication.session.spClient,
    private val concurrency: Int = 10
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns list of Tracks with their metadata
     */
    suspend fun getTrackMetadata(uris: List<String>): List<Track> {
        if (uris.isEmpty()) return emptyList()

        val cachedMap = loadCachedTracks(uris).toMutableMap()

        val missingUris = uris.filterNot { cachedMap.containsKey(it) }
        if (missingUris.isNotEmpty()) {
            val fetched = fetchTracks(missingUris)
            if (fetched.isNotEmpty()) {
                persistMetadata(fetched)
                fetched.forEach { (uri, _) ->
                    cachedMap.remove(uri)
                }
            }
        }

        var tracksWithArtists = loadCachedTracks(uris)
        val toRefetchForAlbum = mutableListOf<String>()

        val albumIdsNeeded = tracksWithArtists.values.mapNotNull { it.track.albumId }.distinct()
        val albumsMap = loadAlbums(albumIdsNeeded)

        val missingAlbumIds = albumIdsNeeded.filter { albumsMap[it] == null }
        if (missingAlbumIds.isNotEmpty()) {
            val albumIdToUris = tracksWithArtists.values
                .groupBy { it.track.albumId ?: "" }
                .mapKeys { it.key }

            missingAlbumIds.forEach { missingAlbumId ->
                val candidates = albumIdToUris[missingAlbumId]
                val pickUri = candidates?.firstOrNull()?.track?.trackUri
                if (pickUri != null) toRefetchForAlbum += pickUri
            }

            if (toRefetchForAlbum.isNotEmpty()) {
                val fetchedAlbumsViaTracks = fetchTracks(toRefetchForAlbum)
                if (fetchedAlbumsViaTracks.isNotEmpty()) {
                    persistMetadata(fetchedAlbumsViaTracks)
                }
            }
        }

        tracksWithArtists = loadCachedTracks(uris)

        val finalAlbumIds = tracksWithArtists.values.mapNotNull { it.track.albumId }.distinct()
        val finalAlbums = loadAlbums(finalAlbumIds)

        // Convert to domain Tracks in the same order as uris
        val result = uris.map { uri ->
            val twa = tracksWithArtists[uri]
                ?: throw RuntimeException("Missing track metadata after fetch/persist for: $uri")

            val albumId = twa.track.albumId ?: ""
            val albumWithArtists = finalAlbums[albumId]
                ?: throw RuntimeException("Missing album metadata for albumId=$albumId (track=$uri)")

            twa.toDomain(albumWithArtists)
        }

        try {
            updateLastAccessed(result)
        } catch (e: Exception) {
            Log.w("Metadata", "Failed to update last accessed", e)
        }

        return result
    }

    /**
     * Returns list of Albums with their metadata
     */
    suspend fun getAlbumMetadata(uris: List<String>): List<Album> = supervisorScope {
        if (uris.isEmpty()) return@supervisorScope emptyList()

        val cachedMap = loadCachedAlbumsByUri(uris).toMutableMap()

        val missingUris = uris.filterNot { cachedMap.containsKey(it) }
        val fetchedAlbums: List<Album> = if (missingUris.isNotEmpty()) {
            val fetched = fetchAlbums(missingUris)
            if (fetched.isNotEmpty()) {
                persistAlbumMetadata(fetched)
            }
            fetched
        } else {
            emptyList()
        }

        // combine cached + fetched, maintaining original order
        uris.map { uri ->
            fetchedAlbums.find { it.uri == uri } ?: cachedMap[uri]?.toDomain()
            ?: throw RuntimeException("Missing album metadata for $uri")
        }
    }


    //region Tracks
    /**
     * Fetches tracks metadata, that aren't cached.
     */
    private suspend fun fetchTracks(uris: List<String>): List<Pair<String, Track>> = supervisorScope{
        if(uris.isEmpty()) return@supervisorScope emptyList()

        val results = mutableListOf<Pair<String, Track>>()
        uris.chunked(concurrency).forEach { chunk ->
            val deferred = chunk.map { uri ->
                async {
                    try {
                        val raw = getNativeMetadata(uri)
                        val t = json.decodeFromString<Track>(raw)
                        uri to t
                    } catch (e: Exception) {
                        Log.e("Metadata", "fetchMissingTracks: failed to fetch: " + uri, e)
                        null
                    }
                }
            }
            results += deferred.awaitAll().filterNotNull()
        }
        results
    }

    /**
     * Loads tracks by given URI
     */
    private suspend fun loadCachedTracks(uris: List<String>): Map<String, TrackWithArtists> {
        if(uris.isEmpty()) return emptyMap()
        return trackDao.getTracksWithArtists(uris).associateBy { it.track.trackUri }
    }

    /**
     * Loads albums by album IDs
     */
    private suspend fun loadAlbums(albumIds: List<String>): Map<String, AlbumWithArtists?> {
        if(albumIds.isEmpty()) return emptyMap()
        val albums = albumDao.getAlbumsWithArtists(albumIds)
        return albums.associateBy { it.album.albumId }
    }

    /**
     * Caches the Tracks in Room database.
     */
    private suspend fun persistMetadata(metadata: List<Pair<String, Track>>) {
        if(metadata.isEmpty()) return

        val now = System.currentTimeMillis()

        val trackEntities = mutableListOf<TrackEntity>()
        val artistEntities = mutableListOf<ArtistEntity>()
        val trackArtistJoins = mutableListOf<TrackArtistEntity>()
        val albumEntities = mutableListOf<AlbumEntity>()
        val albumArtistJoins = mutableListOf<AlbumArtistEntity>()

        metadata.forEach { (_, domainTrack) ->
            val (tEntity, aEntities, joins) = domainTrack.toEntities(now)
            trackEntities += tEntity
            artistEntities += aEntities
            trackArtistJoins += joins

            domainTrack.album?.let { album ->
                val albumEntity = AlbumEntity(
                    albumId = album.id,
                    uri = album.uri,
                    name = album.name,
                    artistNames = album.artists.joinToString(", ") { it.name },
                    coverUri = album.covers.firstOrNull()?.uri,
                    popularity = album.popularity,
                    lastUpdated = now
                )
                albumEntities += albumEntity

                album.artists.forEachIndexed { idx, art ->
                    albumArtistJoins += AlbumArtistEntity(
                        albumId = album.id,
                        artistId = art.id,
                        position = idx
                    )
                }
            }
        }

        db.withTransaction {
            if (artistEntities.isNotEmpty()) artistDao.insertAll(artistEntities)
            if (albumEntities.isNotEmpty()) albumDao.insertAll(albumEntities)
            if (albumArtistJoins.isNotEmpty()) albumArtistDao.insertAll(albumArtistJoins)
            if (trackEntities.isNotEmpty()) trackRepo.upsertTracks(trackEntities, isLibrary = true)
            if (trackArtistJoins.isNotEmpty()) trackArtistDao.insertAll(trackArtistJoins)
        }
    }

    /**
     * Updates the last accessed time for given tracks
     */
    private suspend fun updateLastAccessed(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val now = System.currentTimeMillis()
        val uris = tracks.map { it.uri }
        try {
            trackDao.updateLastAccessedBatch(uris, now)
        } catch (e: Exception) {
            // fallback: touch individually via trackRepo
            uris.forEach { id -> try { trackRepo.touchTrack(id) } catch (_: Exception) {} }
        }
    }
    //endregion Tracks

    //region Albums
    private suspend fun fetchAlbums(uris: List<String>): List<Album> = supervisorScope {
        if (uris.isEmpty()) return@supervisorScope emptyList()

        val results = mutableListOf<Album>()

        uris.chunked(concurrency).forEach { chunk ->
            val deferred = chunk.map { uri ->
                async {
                    try {
                        val raw = getNativeMetadata(uri)
                        println(raw)
                        json.decodeFromString<Album>(raw)
                    } catch (e: Exception) {
                        Log.e("Metadata", "fetchAlbums: failed for $uri", e)
                        null
                    }
                }
            }
            results += deferred.awaitAll().filterNotNull()
        }

        results
    }

    private suspend fun loadCachedAlbumsByUri(
        uris: List<String>
    ): Map<String, AlbumWithArtists> {
        if (uris.isEmpty()) return emptyMap()
        val entities = albumDao.getAlbumsWithArtists(uris)

        return uris.associateWith { uri ->
            val entity = entities.find { it.album.uri == uri }
                ?: return@associateWith null // might be missing

            // map to AlbumWithArtists domain object
            AlbumWithArtists(
                album = entity.album,
                artists = entity.artists
            )
        }.filterValues { it != null } as Map<String, AlbumWithArtists>
    }

    private suspend fun persistAlbumMetadata(albums: List<Album>) {
        if (albums.isEmpty()) return

        val now = System.currentTimeMillis()

        val albumEntities = mutableListOf<AlbumEntity>()
        val albumArtistJoins = mutableListOf<AlbumArtistEntity>()

        albums.forEach { album ->
            albumEntities += AlbumEntity(
                albumId = album.id,
                uri = album.uri,
                name = album.name,
                artistNames = album.artists.joinToString(", ") { it.name },
                coverUri = album.covers.firstOrNull()?.uri,
                popularity = album.popularity,
                lastUpdated = now
            )

            album.artists.forEachIndexed { idx, artist ->
                albumArtistJoins += AlbumArtistEntity(
                    albumId = album.id,
                    artistId = artist.id,
                    position = idx
                )
            }
        }

        db.withTransaction {
            albumDao.insertAll(albumEntities)
            if (albumArtistJoins.isNotEmpty()) {
                albumArtistDao.insertAll(albumArtistJoins)
            }
        }
    }

    //endregion Albums

    /**
     * Native method to get JSON Metadata using URI
     */
    external fun getNativeMetadata(uri: String): String
}