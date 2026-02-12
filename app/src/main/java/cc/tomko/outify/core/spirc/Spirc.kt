package cc.tomko.outify.core.spirc

import android.util.Log
import cc.tomko.outify.OutifyApplication

class Spirc {
    /**
     * Initializes the SpircRuntime
     */
    external fun initializeSpirc(): Boolean

    /**
     * Loads a SpotifyURI
     * @param context valid form of URI, that will get loaded. Leave empty for liked tracks
     * @param playingTrackUri from which to start playing in this context. Leave empty for first/random
     * @return `true` if loaded successfully
     */
    external fun load(context: String? = null, playingTrackUri: String? = null): Boolean

    /**
     * Adds a SpotifyURI to queue
     * @param spotifyUri valid form of URI, that will get loaded
     * @return `true` if loaded successfully
     */
    external fun addToQueue(spotifyUri: String?): Boolean

    /**
     * Activates current Spirc session
     * @return `true` if success
     */
    external fun activate(): Boolean

    /**
     * Transfers current Spirc session
     * @return `true` if success
     */
    external fun transfer(): Boolean

    /**
     * Tells the player to start playing
     */
    external fun playerPlay(): Boolean

    /**
     * Tells the player to pause playing
     */
    external fun playerPause(): Boolean

    /**
     * Tells the player to toggle play status
     */
    external fun playerPlayPause(): Boolean

    /**
     * Tells the player to skip to the next track
     */
    external fun playerNext(): Boolean

    /**
     * Tells the player to play the previous track, or return to the start of current track
     */
    external fun playerPrevious(): Boolean

    /**
     * Gets the previous tracks from queue
     */
    external fun previousTracks(): String

    /**
     * Gets the next tracks from queue
     */
    external fun nextTracks(): String

    /**
     * Called once Spirc session gets initialized.
     * FFI calls this function
     */
    private fun onSpircInitialized() {
        if (!activate()) {
            Log.e("Spirc", "Failed to activate Spirc session!")
            return
        }
        // TODO: Make auto transfer configurable?
        if (!transfer()) {
            Log.e("Spirc", "Failed to transfer Spirc session!")
        }
    }
}