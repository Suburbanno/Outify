package cc.tomko.outify.playback

import android.util.Log
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.data.Metadata
import cc.tomko.outify.data.Track
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AudioManager {
    init {
        registerPcmCallback(this)
    }

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