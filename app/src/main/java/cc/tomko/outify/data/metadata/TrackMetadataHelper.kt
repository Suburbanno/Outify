package cc.tomko.outify.data.metadata

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.data.Playlist
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.album.AlbumArtistEntity
import cc.tomko.outify.data.database.AlbumEntity
import cc.tomko.outify.data.database.album.AlbumTrackCrossRef
import cc.tomko.outify.data.database.album.AlbumWithArtists
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
import cc.tomko.outify.data.toEntities
import cc.tomko.outify.ui.repository.TrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Helper class for track metadata
 */
@Singleton
class TrackMetadataHelper @Inject constructor(
    private val db: AppDatabase,
    private val trackRepo: TrackRepository,
    private val trackDao: TrackDao,
    private val artistDao: ArtistDao,
    private val trackArtistDao: TrackArtistDao,
    private val albumDao: AlbumDao,
    private val albumArtistDao: AlbumArtistDao,
    private val nativeMetadata: NativeMetadata,
    private val json: Json,
    @Named("metadataConcurrency") private val concurrency: Int,
) {
    /**
     * Returns list of Tracks with their metadata
     */
    suspend fun getTrackMetadata(trackUris: List<String>): List<Track> {
        if (trackUris.isEmpty()) return emptyList()
        val uris = trackUris.filter { it.startsWith("spotify:track:") }

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

    suspend fun getTrackAlbumId(trackUri: String): String? {
        return trackDao.getAlbumIdForTrack(trackUri)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTracks(uris: List<String>): Flow<List<Track>> {
        if (uris.isEmpty()) return flowOf(emptyList())

        return flow {
            coroutineScope {
                launch {
                    try {
                        val cached = loadCachedTracks(uris)
                        val missing = uris.filterNot { cached.containsKey(it) }
                        if (missing.isNotEmpty()) {
                            val fetched = fetchTracks(missing)
                            if (fetched.isNotEmpty()) persistMetadata(fetched)
                        }
                    } catch (e: Exception) {
                        Log.w("Metadata", "observeTracks: background fetch failed", e)
                    }
                }
            }

            emitAll(
                trackDao.getTracksWithArtistsFlow(uris)
                    .mapLatest { rows ->

                        if (rows.isEmpty()) return@mapLatest emptyList()

                        val albumIds = rows.mapNotNull { it.track.albumId }.distinct()

                        val albumsMap = loadAlbums(albumIds)

                        // Map rows by trackUri for ordering
                        val rowsByUri = rows.associateBy { it.track.trackUri }

                        // Preserve requested order; skip missing entries
                        uris.mapNotNull { uri ->
                            val twa = rowsByUri[uri] ?: return@mapNotNull null
                            val albumId = twa.track.albumId ?: ""
                            val albumWithArtists =
                                albumsMap[albumId]
                            try {
                                twa.toDomain(albumWithArtists)
                            } catch (e: Exception) {
                                // If domain mapping fails, skip that track
                                null
                            }
                        }
                    }
            )
        }
    }

    /**
     * Fetches tracks metadata, that aren't cached.
     */
    private suspend fun fetchTracks(uris: List<String>): List<Pair<String, Track>> =
        supervisorScope {
            if (uris.isEmpty()) return@supervisorScope emptyList()
            val filtered = uris.filter { it.startsWith("spotify:track:") }

            val results = mutableListOf<Pair<String, Track>>()
            filtered.chunked(concurrency).forEach { chunk ->
                val deferred = chunk.map { uri ->
                    async {
                        try {
                            // Retry on rate limit
                            val raw = nativeMetadata.retryOnRateLimit {
                                nativeMetadata.fetchMetadata(uri)
                            }

                            val t = json.decodeFromString<Track>(raw.toString())
                            uri to t
                        } catch (e: RateLimitException) {
                            Log.w("Metadata", "fetchTracks: rate-limited for $uri, giving up", e)
                            null
                        } catch (e: Exception) {
                            Log.e("Metadata", "fetchTracks: failed for $uri", e)
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
        val albumTrackJoins = mutableListOf<AlbumTrackCrossRef>()

        metadata.forEach { (_, domainTrack) ->
            val (tEntity, aEntities, joins) = domainTrack.toEntities(now)
            trackEntities += tEntity
            artistEntities += aEntities
            trackArtistJoins += joins

            domainTrack.album?.let { album ->
                val sortedByArea = album.covers
                    .sortedBy { it.width * it.height }

                val small = sortedByArea.firstOrNull()
                val large = sortedByArea.lastOrNull()
                val medium = sortedByArea.getOrNull(sortedByArea.size / 2)


                val albumEntity = AlbumEntity(
                    albumId = album.id,
                    uri = album.uri,
                    name = album.name,
                    artistNames = album.artists.joinToString(", ") { it.name },
                    popularity = album.popularity,
                    lastUpdated = now,

                    smallCoverUri = small?.uri,
                    mediumCoverUri = medium?.uri,
                    largeCoverUri = large?.uri
                )
                albumEntities += albumEntity

                album.artists.forEachIndexed { idx, art ->
                    albumArtistJoins += AlbumArtistEntity(
                        albumId = album.id,
                        artistId = art.id,
                        position = idx
                    )
                }

                album.tracks.forEachIndexed { index, track ->
                    albumTrackJoins += AlbumTrackCrossRef(
                        albumId = album.id,
                        trackId = track,
                        position = index,
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
}