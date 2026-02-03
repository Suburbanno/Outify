package cc.tomko.outify.ui.repository

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.*
import cc.tomko.outify.data.database.dao.*
import cc.tomko.outify.data.toEntities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class LibraryRepository(
    private val db: AppDatabase,
    private val trackRepo: TrackRepository,
    private val trackDao: TrackDao,
    private val artistDao: ArtistDao,
    private val trackArtistDao: TrackArtistDao,
    private val albumDao: AlbumDao,
    private val albumArtistDao: AlbumArtistDao,
    private val metadata: Metadata = Metadata(),
    private val spClient: SpClient = OutifyApplication.session.spClient,
    private val concurrency: Int = 10
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLikedTracks(limit: Int, offset: Int): List<Track> =
        withContext(Dispatchers.IO) {
            supervisorScope {
                val allUris = fetchLikedTrackUris()
                if (allUris.isEmpty()) return@supervisorScope emptyList()

                val pageUris = paginate(allUris, limit, offset)
                if (pageUris.isEmpty()) return@supervisorScope emptyList()

                val cachedMap = loadCachedTracks(pageUris)
                val missingUris = pageUris.filter { it !in cachedMap.keys }

                val fetchedPairs = fetchMissingTracks(missingUris)

                persistFetchedMetadata(fetchedPairs)

                val albumMap = loadAlbumsForTracks(pageUris, cachedMap, fetchedPairs)

                val result = buildResultList(pageUris, cachedMap, fetchedPairs, albumMap)

                updateLastAccessed(result)

                result
            }
        }

    // -------------------------
    // Helper functions
    // -------------------------

    private suspend fun fetchLikedTrackUris(): List<String> {
        val jsonUris = spClient.getLikedSongs(0, 1)
        return json.decodeFromString(jsonUris)
    }

    private fun paginate(all: List<String>, limit: Int, offset: Int): List<String> {
        val from = offset.coerceAtMost(all.size)
        val to = (offset + limit).coerceAtMost(all.size)
        return all.subList(from, to)
    }

    private suspend fun loadCachedTracks(uris: List<String>): Map<String, TrackWithArtists> {
        if (uris.isEmpty()) return emptyMap()
        val list = trackDao.getTracksWithArtists(uris)
        return list.associateBy { it.track.trackUri }
    }

    private suspend fun fetchMissingTracks(uris: List<String>): List<Pair<String, Track>> = supervisorScope {
        if (uris.isEmpty()) return@supervisorScope emptyList()

        val results = mutableListOf<Pair<String, Track>>()
        uris.chunked(concurrency).forEach { chunk ->
            val deferred = chunk.map { uri ->
                async {
                    try {
                        val raw = metadata.getMetadata(uri)
                        val t = json.decodeFromString<Track>(raw)
                        uri to t
                    } catch (e: Exception) {
                        Log.e("LibraryRepository", "fetchMissingTracks: failed to fetch: " + uri, e)
                        null
                    }
                }
            }
            results += deferred.awaitAll().filterNotNull()
        }
        results
    }

    private suspend fun persistFetchedMetadata(fetched: List<Pair<String, Track>>) {
        if (fetched.isEmpty()) return

        val now = System.currentTimeMillis()

        val trackEntities = mutableListOf<TrackEntity>()
        val artistEntities = mutableListOf<ArtistEntity>()
        val trackArtistJoins = mutableListOf<TrackArtistEntity>()
        val albumEntities = mutableListOf<AlbumEntity>()
        val albumArtistJoins = mutableListOf<AlbumArtistEntity>()

        fetched.forEach { (_, domainTrack) ->
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

    private suspend fun loadAlbumsForTracks(
        pageUris: List<String>,
        cachedMap: Map<String, TrackWithArtists>,
        fetchedPairs: List<Pair<String, Track>>
    ): Map<String, AlbumWithArtists?> {
        // collect albumIds from cache and fetched
        val cachedAlbumIds = cachedMap.values.mapNotNull { it.track.albumId }
        val fetchedAlbumIds = fetchedPairs.mapNotNull { it.second.album?.id }
        val albumIds = (cachedAlbumIds + fetchedAlbumIds).distinct()
        if (albumIds.isEmpty()) return emptyMap()
        val albums = albumDao.getAlbumsWithArtists(albumIds)
        return albums.associateBy { it.album.albumId }
    }

    private fun buildResultList(
        pageUris: List<String>,
        cachedMap: Map<String, TrackWithArtists>,
        fetchedPairs: List<Pair<String, Track>>,
        albumMap: Map<String, AlbumWithArtists?>
    ): List<Track> {
        // Map fetched by uri for quick lookup
        val fetchedByUri = fetchedPairs.toMap()

        return pageUris.mapNotNull { uri ->
            // Prefer cached DB entry (mapped to domain), else returned fetched domain track
            val cached = cachedMap[uri]
            if (cached != null) {
                val albumWithArtists = cached.track.albumId?.let { albumMap[it] }
                cached.toDomain(albumWithArtists)
            } else {
                fetchedByUri[uri] // may be null if fetch failed
            }
        }
    }

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
