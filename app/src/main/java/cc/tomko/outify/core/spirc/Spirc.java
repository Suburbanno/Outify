package cc.tomko.outify.core.spirc;

import android.util.Log;

public class Spirc {
    /**
     * Initializes the SpircRuntime
     */
    public native boolean initializeSpirc();

    /**
     * Loads a SpotifyURI
     * @param spotifyUri valid form of URI, that will get loaded
     * @return <code>true</code> if loaded successfully
     */
    public native boolean load(String spotifyUri);

    /**
     * Activates current Spirc session
     * @return <code>true</code> if success
     */
    public native boolean activate();

    /**
     * Transfers current Spirc session
     * @return <code>true</code> if success
     */
    public native boolean transfer();

    /**
     * Called once Spirc session gets initialized
     */
    private void onSpircInitialized(){
        if (!activate()){
            Log.e("Spirc", "Failed to activate Spirc session!");
            return;
        }
        // TODO: Make auto transfer configurable?
        if(!transfer()){
            Log.e("Spirc", "Failed to transfer Spirc session!");
        }
    }
}
