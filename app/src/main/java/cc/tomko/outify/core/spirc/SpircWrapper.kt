package cc.tomko.outify.core.Spirc

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.spirc.ISpircWrapper
import cc.tomko.outify.core.spirc.Spirc
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.services.MusicService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class SpircWrapper @Inject constructor(
    @ApplicationContext val context: Context,
    private val playbackStateHolder: PlaybackStateHolder,
): ISpircWrapper{
    var isShuffling = false
    var isRepeating = false

    /**
     * Whether Spirc is in usable state, so we can query it
     */
    var isUsable = false

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

    /**
     * Shuffles the playback
     * @return <code>true</code> if success
     */
    override fun shuffle(enabled: Boolean): Boolean {
        isShuffling = enabled
        return Spirc.shuffle(enabled)
    }

    /**
     * Repeats the playback
     * @return <code>true</code> if success
     */
    override fun repeat(enabled: Boolean): Boolean {
        isRepeating = enabled
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