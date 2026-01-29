package cc.tomko.outify.playback

import android.util.Log
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import kotlinx.serialization.json.Json

/**
 * Manages the playback state, plays tracks, ..
 */
class PlaybackManager {
    val playbackStateHolder = PlaybackStateHolder()

    init {
        registerCallbacks()
    }

    /**
     * Registers rust state callbacks
     */
    fun registerCallbacks(){
        // TODO: Modularize
        registerPlayerEventListener(object: PlayerEventCallback {
            override fun onPlaying(
                spotify_uri: String,
                position_ms: Long,
                play_request_id: Long,
                json_raw: String,
            ) {
                val json = Json { ignoreUnknownKeys = true }
                val track: Track = json.decodeFromString(json_raw)
                playbackStateHolder.onTrackChanged(track)
            }
        })
    }

    /**
     * Registers PlayerEvent listener to FFI.
     * FFI stores the GlobalRef of the callback
     */
    external fun registerPlayerEventListener(callback: PlayerEventCallback);
}