package cc.tomko.outify.playback.callbacks

/**
 * Callback for all implemented PlayerEvent callbacks
 */
interface PlayerEventCallback {
    /**
     * Called when:
     *  - a new track is played
     *  - track is resumed
     *  - after a seek
     *  - after buffer-underrun
     */
    fun onPlaying(spotify_uri: String, position_ms: Long, play_request_id: Long, json: String);
}