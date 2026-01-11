package cc.tomko.outify;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class AuthStorage {

    private static final String PREF_FILE = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private final SharedPreferences sharedPreferences;

    public AuthStorage(Context context) throws Exception {
        // Create or retrieve the Master Key for encryption
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        // Initialize EncryptedSharedPreferences
        sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREF_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    // Save access token
    public void saveAccessToken(String token) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    // Read access token
    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    // Save refresh token
    public void saveRefreshToken(String token) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply();
    }

    // Read refresh token
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    // Clear all tokens
    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}

