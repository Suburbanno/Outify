package cc.tomko.outify.ui.repository

import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.Lyrics
import cc.tomko.outify.data.LyricsResponse
import cc.tomko.outify.data.SyncedLyric
import cc.tomko.outify.data.Track
import kotlinx.serialization.json.Json
import java.lang.Exception
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val spClient: SpClient,
    private val json: Json,
) {
    fun getLyrics(track: Track?): List<SyncedLyric> {
        val id = track?.id ?: return emptyList()
        val raw = spClient.getLyrics(id)

        try {
            val response: LyricsResponse = json.decodeFromString(raw)
            val lyrics = response.lyrics

            return lyrics.lines
                .filter { it.words.isNotBlank() }
                .map {
                    SyncedLyric(
                        timeMs = it.startTimeMs.toLong(),
                        text = it.words
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
