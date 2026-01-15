package cc.tomko.outify.playback;

import android.util.Log;
import androidx.annotation.NonNull;

public class AudioManager {
    public AudioManager(){
        registerPcmCallback(this);
    }

    /**
     * Initializes and connects the session
     * @param access_token to authorize into users account
     */
    public native void initializeSession(String access_token);

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

    private static void onNativePcm(byte[] data, int sampleRate, int channels, int format){
        System.out.println("Received PCM: " + data.length + " bytes, " + sampleRate + " Hz, " + channels + " channels");
    }

    private static native void registerPcmCallback(AudioManager callbackPtr);
}
