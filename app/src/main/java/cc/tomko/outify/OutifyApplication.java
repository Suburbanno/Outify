package cc.tomko.outify;

import android.app.Application;
import android.util.Log;

import cc.tomko.outify.core.Session;
import cc.tomko.outify.core.SpAuthManager;
import cc.tomko.outify.playback.AudioManager;
import com.google.crypto.tink.aead.AeadConfig;

import java.security.GeneralSecurityException;

public class OutifyApplication extends Application {
    public static SecureStorage secureStorage;

    public static SpAuthManager spAuthManager;
    public static AudioManager audioManager;
    public static Session session;

    @Override
    public void onCreate() {
        super.onCreate();

        System.loadLibrary("librespot_ffi");
        LibrespotFfi.libInit(getApplicationContext());

        try {
            AeadConfig.register();
            secureStorage = new SecureStorage(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        session = new Session();
        session.initializeSession();

//        try {
//            debug.debug1(secureStorage.getString(SecureStorage.Keys.ACCESS_TOKEN));
//        } catch (GeneralSecurityException e) {
//            throw new RuntimeException(e);
//        }

        audioManager = new AudioManager();

        // Initializing SpAuthManager
        spAuthManager = new SpAuthManager();
    }
}
