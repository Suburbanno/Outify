package cc.tomko.outify.core.spirc

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.services.MusicService

class SpircWrapper(val context: Context) {
    val spirc: Spirc = Spirc()

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
    fun load(context: String? = null, playingTrackUri: String? = null): Boolean {
        ensureServiceRunning()
        return spirc.load(context, playingTrackUri)
    }

    /**
     * Adds a SpotifyURI to queue
     * @param spotifyUri valid form of URI, that will get loaded
     * @return `true` if loaded successfully
     */
    fun addToQueue(spotifyUri: String?): Boolean {
        return spirc.addToQueue(spotifyUri)
    }

    /**
     * Activates current Spirc session
     * @return `true` if success
     */
    fun activate(): Boolean {
        return spirc.activate()
    }

    /**
     * Transfers current Spirc session
     * @return `true` if success
     */
    fun transfer(): Boolean {
        return spirc.transfer()
    }

    /**
     * Tells the player to start playing
     */
    fun playerPlay(): Boolean {
        ensureServiceRunning()
        return spirc.playerPlay()
    }

    /**
     * Tells the player to pause playing
     */
    fun playerPause(): Boolean {
        ensureServiceRunning()
        return spirc.playerPause()
    }

    /**
     * Tells the player to toggle play status
     */
    fun playerPlayPause(): Boolean {
        ensureServiceRunning()
        return spirc.playerPlayPause()
    }

    /**
     * Tells the player to skip to the next track
     */
    fun playerNext(): Boolean {
        return spirc.playerNext()
    }

    /**
     * Tells the player to play the previous track, or return to the start of current track
     */
    fun playerPrevious(): Boolean {
        return spirc.playerPrevious()
    }

    /**
     * Gets the previous tracks from queue
     */
    fun previousTracks(): String {
        return spirc.previousTracks()
    }

    /**
     * Gets the next tracks from queue
     */
    fun nextTracks(): String {
        return spirc.nextTracks()
    }
}