package cc.tomko.outify.playback;

import android.util.Log;
import androidx.annotation.NonNull;

public class AudioManager {
    public AudioManager(){
        registerPcmCallback(this);
    }

    private static void onNativePcm(byte[] data, int sampleRate, int channels, int format){
        System.out.println("Received PCM: " + data.length + " bytes, " + sampleRate + " Hz, " + channels + " channels");
    }

    private static native void registerPcmCallback(AudioManager callbackPtr);
}
