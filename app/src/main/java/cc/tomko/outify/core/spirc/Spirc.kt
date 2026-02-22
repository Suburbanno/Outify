package cc.tomko.outify.core.spirc

import android.util.Log
import cc.tomko.outify.OutifyApplication

interface SpircInitializationCallback {
    /**
     * Called when Spirc initializes
     */
    fun initialized()

    /**
     * Called when Spirc fails to initialize
     */
    fun failed()
}

object Spirc {
    /**
     * Initializes the SpircRuntime
     */
    @JvmStatic
    external fun initializeSpirc(callback: SpircInitializationCallback): Boolean

    /**
     * Loads a SpotifyURI
     * @param context valid form of URI, that will get loaded. Leave empty for liked tracks
     * @param playingTrackUri from which to start playing in this context. Leave empty for first/random
     * @return `true` if loaded successfully
     */
    @JvmStatic
    external fun load(context: String? = null, playingTrackUri: String? = null): Boolean

    /**
     * Loads the context URI and starts playing randomly within it
     */
    @JvmStatic
    external fun shuffleLoad(uri: String): Boolean

    /**
     * Adds a SpotifyURI to queue
     * @param spotifyUri valid form of URI, that will get loaded
     * @return `true` if loaded successfully
     */
    @JvmStatic
    external fun addToQueue(spotifyUri: String?): Boolean

    /**
     * Activates current Spirc session
     * @return `true` if success
     */
    @JvmStatic
    external fun activate(): Boolean

    /**
     * Transfers current Spirc session
     * @return `true` if success
     */
    @JvmStatic
    external fun transfer(): Boolean

    /**
     * Seeks the current track to given position
     * @return `true` if success
     */
    @JvmStatic
    external fun seekTo(positionMs: Long): Boolean

    /**
     * Shuffles the playback
     * @return <code>true</code> if success
     */
    @JvmStatic
    external fun shuffle(enabled: Boolean): Boolean

    /**
     * Repeats the playback
     * @return <code>true</code> if success
     */
    @JvmStatic
    external fun repeat(enabled: Boolean): Boolean

    /**
     * Tells the player to start playing
     */
    @JvmStatic
    external fun playerPlay(): Boolean

    /**
     * Tells the player to pause playing
     */
    @JvmStatic
    external fun playerPause(): Boolean

    /**
     * Tells the player to toggle play status
     */
    @JvmStatic
    external fun playerPlayPause(): Boolean

    /**
     * Tells the player to skip to the next track
     */
    @JvmStatic
    external fun playerNext(): Boolean

    /**
     * Tells the player to play the previous track, or return to the start of current track
     */
    @JvmStatic
    external fun playerPrevious(): Boolean

    /**
     * Gets the previous tracks from queue
     */
    @JvmStatic
    external fun previousTracks(): String

    /**
     * Gets the next tracks from queue
     */
    @JvmStatic
    external fun nextTracks(): String
}