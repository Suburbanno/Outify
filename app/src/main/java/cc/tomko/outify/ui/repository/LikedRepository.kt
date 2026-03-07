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
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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

    /**
     * Pulls the URI list from the API and rebuilds liked_songs if it differs.
     * Returns true if anything changed.
     */
    suspend fun syncLikedUris(): Boolean {
        val remote = metadata.getLikedUris()
        val cached = likedDao.getLikedIds()
        if (remote == cached) return false

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