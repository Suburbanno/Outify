package cc.tomko.outify;

import android.app.Application;
import cc.tomko.outify.core.SpAuthManager;
import com.google.crypto.tink.aead.AeadConfig;

public class OutifyApplication extends Application {
    /**
     * Contains the Auth tokens
     */
    public static TokenStore tokenStore;

    public static SpAuthManager spAuthManager;

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

        // Initializing SpAuthManager
        spAuthManager = new SpAuthManager();
        spAuthManager.initialize("", "", "");
    }
}
