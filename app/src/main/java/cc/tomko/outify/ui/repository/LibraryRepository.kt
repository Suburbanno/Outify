package cc.tomko.outify.ui.repository

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.*
import cc.tomko.outify.data.database.dao.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class LibraryRepository(
    private val metadata: Metadata,
    private val spClient: SpClient = OutifyApplication.session.spClient,
) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedLikedUris: List<String>? = null


    suspend fun getLikedTracks(limit: Int, offset: Int): List<Track> =
        withContext(Dispatchers.IO) {
            supervisorScope {
                val allUris = fetchLikedTrackUris()
                if (allUris.isEmpty()) return@supervisorScope emptyList()

                val pageUris = paginate(allUris, limit, offset)
                if (pageUris.isEmpty()) return@supervisorScope emptyList()

                val result = metadata.getTrackMetadata(pageUris)
                result
            }
        }

    private suspend fun fetchLikedTrackUris(): List<String> {
        cachedLikedUris?.let { return it }

        val jsonUris = spClient.getUserCollection()
        val parsed = json.decodeFromString<List<String>>(jsonUris)
        cachedLikedUris = parsed
        return parsed
    }

    private fun paginate(all: List<String>, limit: Int, offset: Int): List<String> {
        val from = offset.coerceAtMost(all.size)
        val to = (offset + limit).coerceAtMost(all.size)
        return all.subList(from, to)
    }
}
