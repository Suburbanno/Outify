package cc.tomko.outify;

import android.app.Application;
import cc.tomko.outify.core.SpAuthManager;
import cc.tomko.outify.playback.AudioManager;
import com.google.crypto.tink.aead.AeadConfig;

public class OutifyApplication extends Application {
    public static SecureStorage secureStorage;

    public static SpAuthManager spAuthManager;
    public static AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();

        System.loadLibrary("librespot_ffi");
        LibrespotFfi.libInit();

        try {
            AeadConfig.register();
            secureStorage = new SecureStorage(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        audioManager = new AudioManager();

        // Initializing SpAuthManager
        spAuthManager = new SpAuthManager();
        spAuthManager.initialize("", "", "");
    }
}
