package cc.tomko.outify.playback;

import android.util.Log;
import androidx.annotation.NonNull;

import cc.tomko.outify.OutifyApplication;

public class AudioManager {
    public AudioManager(){
        registerPcmCallback(this);
    }

    /**
     * This function is called from JNI upon each received PCM data.
     */
    private void onNativePcm(byte[] data, int sampleRate, int channels, int format){
        // S16
        if(format == 5){
            OutifyApplication.audioPlayer.onPCM(data, sampleRate, channels, AudioFormat.S16);
        }
        // TODO: Implement more formats
    }

    private static native void registerPcmCallback(AudioManager callbackPtr);
}
