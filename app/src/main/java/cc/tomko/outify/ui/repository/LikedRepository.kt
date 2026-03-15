package cc.tomko.outify.ui.repository

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.TrackWithArtists
import cc.tomko.outify.data.database.album.AlbumWithArtists
import cc.tomko.outify.data.database.dao.AlbumDao
import cc.tomko.outify.data.database.dao.LikedDao
import cc.tomko.outify.data.database.impl.LikedTrackEntity
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.metadata.TrackMetadataHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class LikedRepository @Inject constructor(
    private val db: AppDatabase,
    private val likedDao: LikedDao,
    private val albumDao: AlbumDao,
    private val trackMetadataHelper: TrackMetadataHelper,
    private val metadata: Metadata,
) {
    companion object {
        private const val TAG = "LikedRepository"
        private const val SUBSTRING_OFFSET = "spotify:track:".length
    }

    suspend fun syncLikedTracks(): Boolean {
        val pageSize = 50
        val perPageDelayMs = 200L
        val maxRetries = 3
        val initialBackoffMs = 500L

        try {
            try {
                if(!syncLikedUris()) return false
            } catch (t: Throwable) {
                Log.w(TAG, "syncLikedUris failed (continuing): ${t.message}", t)
            }

            var offset = 0
            var anyFetched = false

            while (true) {
                val ids = likedDao.getIdsWindow(limit = pageSize, offset = offset)
                if (ids.isEmpty()) break

                val uris = ids.map { "spotify:track:$it" }

                var attempt = 0
                var succeeded = false
                var backoff = initialBackoffMs

                while (attempt < maxRetries && !succeeded) {
                    try {
                        val fetched = trackMetadataHelper.getTrackMetadata(uris)

                        if (fetched.isNotEmpty()) {
                            anyFetched = true
                        }

                        succeeded = true
                    } catch (e: Exception) {
                        attempt++
                        val isTransient = true
                        if (attempt >= maxRetries || !isTransient) {
                            Log.e(TAG, "Failed fetching metadata for liked tracks (offset=$offset).", e)
                            return false
                        } else {
                            Log.w(TAG, "Transient failure fetching metadata (offset=$offset), retrying in $backoff ms (attempt=$attempt).", e)
                            delay(backoff)
                            backoff = min(backoff * 2, 10_000L)
                        }
                    }
                }

                // polite pause between pages
                delay(perPageDelayMs)
                offset += pageSize
            }

            Log.d(TAG, "syncLikedTracks finished; anyFetched=$anyFetched")
            return true
        } catch (t: Throwable) {
            Log.e("LikedRepository", "syncLikedTracks failed unexpectedly", t)
            return false
        }
    }

    /**
     * Pulls the URI list from the API and rebuilds liked_songs if it differs.
     * Returns true if anything changed.
     */
    suspend fun syncLikedUris(): Boolean {
        val remote = metadata.getLikedUris()
        val cached = likedDao.getLikedIds()

        if (remote.size == cached.size) {
            var allMatch = true
            for ((i, uri) in remote.withIndex()) {
                if (uri.length <= SUBSTRING_OFFSET || uri.substring(SUBSTRING_OFFSET) != cached[i]) {
                    allMatch = false
                    break
                }
            }
            if (allMatch) return false
        }

        Log.d(TAG, "Liked list changed (${cached.size} → ${remote.size}), resyncing")
        db.withTransaction {
            likedDao.clearAll()
            val now = System.currentTimeMillis()
            remote.forEachIndexed { i, uri ->
                likedDao.insert(
                    LikedTrackEntity(
                        trackId = uri.substring(SUBSTRING_OFFSET),
                        position = i.toDouble(),
                        addedAt = now,
                    )
                )
            }
        }
        return true
    }

    /**
     * Emits TrackWithArtists rows for every liked song that has metadata cached.
     */
    fun observeLikedTracksWithDetails(): Flow<List<TrackWithArtists>> =
        likedDao.observeLikedTracksWithDetails()

    fun observeSearchLikedTracks(query: String): Flow<List<TrackWithArtists>> =
        likedDao.observeSearchLikedTracks(query)

    fun observeCount(): Flow<Int> = likedDao.observeCount()

    /**
     * Fetches (or confirms cached) metadata for a window of liked tracks.
     */
    suspend fun ensureWindowLoaded(offset: Int, size: Int) {
        val ids = likedDao.getIdsWindow(limit = size, offset = offset)
        if (ids.isNotEmpty()) {
            trackMetadataHelper.getTrackMetadata(ids.map { "spotify:track:$it" })
        }
    }

    suspend fun getAlbumsForTracks(tracks: List<TrackWithArtists>): Map<String, AlbumWithArtists?> {
        val albumIds = tracks.mapNotNull { it.track.albumId }.distinct()
        if (albumIds.isEmpty()) return emptyMap()
        return albumDao.getAlbumsWithArtists(albumIds).associateBy { it.album.albumId }
    }
}