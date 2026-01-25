package cc.tomko.outify.playback;

import android.util.Log;
import androidx.annotation.NonNull;

import cc.tomko.outify.OutifyApplication;

public class AudioManager {
    public AudioManager(){
        registerPcmCallback(this);
    }

    /**
     * Initializes and connects the session
     */
    public native void initializeSession(SessionInitializationCallback callback);

    /**
     * Initializes the player and the AndroidSink
     */
    public native void initializePlayer();

    /**
     * Temporary function
     * Plays a track - which should get passed into the AndroidSink and then into {@link #onNativePcm(byte[], int, int, int)} with PCM data.
     * @param track_id base62 track id
     */
    public native void playTrack(String track_id);

    public void onNativePcm(byte[] data, int sampleRate, int channels, int format){
        // S16
        if(format == 5){
            OutifyApplication.audioPlayer.onPCM(data, sampleRate, channels, AudioFormat.S16);
        }
        // TODO: Implement more formats
    }

    private static native void registerPcmCallback(AudioManager callbackPtr);
}
