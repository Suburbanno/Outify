package cc.tomko.outify.playback

import android.util.Log
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import kotlinx.serialization.json.Json

class PlaybackManager {
    val playbackStateHolder = PlaybackStateHolder()

    init {
        registerCallbacks()
    }

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

    external fun registerPlayerEventListener(callback: PlayerEventCallback);
}