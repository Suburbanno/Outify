package cc.tomko.outify.playback;

/**
 * Called when Playback session is initialized
 */
public interface SessionInitializationCallback {
    void onConnected();
    void onError(String message);
}
