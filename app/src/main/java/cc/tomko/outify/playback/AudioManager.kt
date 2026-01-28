package cc.tomko.outify.playback

import android.util.Log
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import cc.tomko.outify.playback.callbacks.TrackUpdateCallback

class AudioManager {
    constructor() {
        registerPcmCallback(this)

        registerPlayerEventListener(object: PlayerEventCallback {
            override fun onPlaying(
                spotify_uri: String,
                position_ms: Long,
                play_request_id: Long,
                json: String,
            ) {
                Log.i("AudioManager", "Playing " + spotify_uri)
                Log.i("AudioManager", "onPlaying: " + json);
            }

        })
    }

    external fun registerPlayerEventListener(callback: PlayerEventCallback);

    //region PCM
    /**
     * This function is called from JNI upon each received PCM data.
     */
    private fun onNativePcm(data: ByteArray?, sampleRate: Int, channels: Int, format: Int) {
        // S16
        if (format == 5) {
            OutifyApplication.audioPlayer.onPCM(data, sampleRate, channels, AudioFormat.S16)
        }
        // TODO: Implement more formats
    }

    private external fun registerPcmCallback(callbackPtr: AudioManager?) //endregion


}