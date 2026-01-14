package cc.tomko.outify;

import android.app.Application;
import cc.tomko.outify.core.SpAuthManager;
import cc.tomko.outify.playback.AudioManager;
import com.google.crypto.tink.aead.AeadConfig;

public class OutifyApplication extends Application {
    /**
     * Contains the Auth tokens
     */
    public static TokenStore tokenStore;

    public static SpAuthManager spAuthManager;
    public static AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();

        System.loadLibrary("librespot_ffi");
        LibrespotFfi.libInit();

        try {
            AeadConfig.register();
            tokenStore = new TokenStore(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        audioManager = new AudioManager();

        // Initializing SpAuthManager
        spAuthManager = new SpAuthManager();
        spAuthManager.initialize("", "", "");
    }
}
