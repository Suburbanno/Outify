package cc.tomko.outify.ui.repository

import androidx.room.withTransaction
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.dao.*
import cc.tomko.outify.data.database.impl.LikedTrackEntity
import cc.tomko.outify.data.database.toDomain
import cc.tomko.outify.data.metadata.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.min
import kotlin.system.measureTimeMillis

class LibraryRepository @Inject constructor(
    private val db: AppDatabase,
    private val metadata: Metadata,
    private val likedDao: LikedDao,
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao,             // <- DAO that persists TrackEntity
    private val artistDao: ArtistDao,           // <- DAO for ArtistEntity
    private val trackArtistDao: TrackArtistDao, // <- join DAO (trackId, artistId)
    private val spClient: SpClient,
    private val json: Json,
) {
}