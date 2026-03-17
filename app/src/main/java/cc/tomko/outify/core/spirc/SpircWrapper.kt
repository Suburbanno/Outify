package cc.tomko.outify.core.Spirc

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.RadioResult
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.spirc.ISpircWrapper
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.services.MusicService
import cc.tomko.outify.ui.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class SpircWrapper @Inject constructor(
    @ApplicationContext val context: Context,
    private val playbackStateHolder: PlaybackStateHolder,
    private val spClient: SpClient,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
): ISpircWrapper{
    val scope = CoroutineScope(
        Dispatchers.Main.immediate
    )

    /**
     * Whether Spirc is in usable state, so we can query it
     */
    var isUsable = false

    override fun startRadio(trackUri: String, shuffle: Boolean): Boolean {
        val jsonResult = spClient.getRadioForTrack(trackUri)
        val result: RadioResult = json.decodeFromString(jsonResult)

        if(result.total == 0 || result.mediaItems.isEmpty()){
            return false
        }

        val playlistUri = result.mediaItems.first().uri

        if(shuffle) {
            shuffleLoad(playlistUri)
        } else {
            load(playlistUri, trackUri)
        }

        return true
    }

    @OptIn(UnstableApi::class)
    private fun ensureServiceRunning() {
        val intent = Intent(context, MusicService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }


    /**
     * Loads a SpotifyURI
     * @param context valid form of URI, that will get loaded. Leave empty for liked tracks
     * @param playingTrackUri from which to start playing in this context. Leave empty for first/random
     * @return `true` if loaded successfully
     */
    override fun load(context: String?, playingTrackUri: String?): Boolean {
        ensureServiceRunning()
        return Spirc.load(context, playingTrackUri)
    }

    override fun setQueue(uris: Array<String>): Boolean {
        ensureServiceRunning()
        return Spirc.setQueue(uris)
    }

    override fun localLoad(uri: String): Boolean {
        ensureServiceRunning()
        return Spirc.localLoad(uri)
    }

    /**
     * Shuffles the playback
     * @return <code>true</code> if success
     */
    override fun shuffle(enabled: Boolean): Boolean {
        scope.launch {
            settingsRepository.setShuffle(enabled)
        }
        return Spirc.shuffle(enabled)
    }

    /**
     * Repeats the playback
     * @return <code>true</code> if success
     */
    override fun repeat(enabled: Boolean): Boolean {
        scope.launch {
            settingsRepository.setRepeat(enabled)
        }
        return Spirc.repeat(enabled)
    }

    /**
     * Loads the context URI and starts playing randomly within it
     */
    override fun shuffleLoad(uri: String?): Boolean {
        ensureServiceRunning()
        return Spirc.shuffleLoad(uri)
    }

    /**
     * Adds a SpotifyURI to queue
     * @param spotifyUri valid form of URI, that will get loaded
     * @return `true` if loaded successfully
     */
    override fun addToQueue(spotifyUri: String?): Boolean {
        // TODO: cache in kotlin, so we can have faster UX
        return Spirc.addToQueue(spotifyUri)
    }

    /**
     * Activates current Spirc session
     * @return `true` if success
     */
    override fun activate(): Boolean {
        return Spirc.activate()
    }

    /**
     * Transfers current Spirc session
     * @return `true` if success
     */
    override fun transfer(): Boolean {
        return Spirc.transfer()
    }

    /**
     * Seeks the current track to given position
     * @return `true` if success
     */
    override suspend fun seekTo(positionMs: Long): Boolean {
        if(positionMs < 0){
            return false
        }

        // Assuming it went successfully - pre-updating the position
        playbackStateHolder.seekTo(positionMs.toDuration(DurationUnit.MILLISECONDS))

        return Spirc.seekTo(positionMs)
    }

    /**
     * Tells the player to start playing
     */
    override fun playerPlay(): Boolean {
        ensureServiceRunning()
        return Spirc.playerPlay()
    }

    /**
     * Tells the player to pause playing
     */
    override fun playerPause(): Boolean {
        ensureServiceRunning()
        return Spirc.playerPause()
    }

    /**
     * Tells the player to toggle play status
     */
    override fun playerPlayPause(): Boolean {
        ensureServiceRunning()
        return Spirc.playerPlayPause()
    }

    /**
     * Tells the player to skip to the next track
     */
    override fun playerNext(): Boolean {
        return Spirc.playerNext()
    }

    /**
     * Tells the player to play the previous track, or return to the start of current track
     */
    override fun playerPrevious(): Boolean {
        return Spirc.playerPrevious()
    }

    /**
     * Gets the previous tracks from queue
     */
    override fun previousTracks(): String {
        return Spirc.previousTracks()
    }

    /**
     * Gets the next tracks from queue
     */
    override fun nextTracks(): String {
        return Spirc.nextTracks()
    }
}