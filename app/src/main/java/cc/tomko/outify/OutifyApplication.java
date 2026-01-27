package cc.tomko.outify;

import android.app.Application;

import cc.tomko.outify.core.Session;
import cc.tomko.outify.core.AuthManager;
import cc.tomko.outify.core.spirc.Spirc;
import cc.tomko.outify.playback.AudioManager;
import cc.tomko.outify.playback.AudioPlayer;

import com.google.crypto.tink.aead.AeadConfig;

public class OutifyApplication extends Application {
    public static SecureStorage secureStorage;

    public static AuthManager spAuthManager;
    public static AudioManager audioManager;
    public static AudioPlayer audioPlayer;
    public static Session session;
    public static Spirc spirc;

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

        audioPlayer = new AudioPlayer();

        session = new Session();
//        session.initializeSession();

//        try {
//            debug.debug1(secureStorage.getString(SecureStorage.Keys.ACCESS_TOKEN));
//        } catch (GeneralSecurityException e) {
//            throw new RuntimeException(e);
//        }

        audioManager = new AudioManager();

        // Initializing SpAuthManager
        spAuthManager = new AuthManager();
    }
}
