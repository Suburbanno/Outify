package cc.tomko.outify.playback

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
            override fun onTrackChange(spotify_uri: String, json_raw: String) {
                val json = Json { ignoreUnknownKeys = true }
                val track: Track = json.decodeFromString(json_raw)

                playbackStateHolder.onTrackChanged(track)
            }

            override fun onPositionUpdate(
                spotify_uri: String,
                position_ms: Long,
                json_raw: String
            ) {
                if(playbackStateHolder.currentTrack.value?.id != spotify_uri){
                    onTrackChange(spotify_uri, json_raw)
                }

                playbackStateHolder.onPositionUpdate(position_ms)
            }

            override fun onPlayingStatus(playing: Boolean) {
                playbackStateHolder.isPlaying.value = playing
            }
        })
    }

    /**
     * Registers PlayerEvent listener to FFI.
     * FFI stores the GlobalRef of the callback
     */
    external fun registerPlayerEventListener(callback: PlayerEventCallback);
}