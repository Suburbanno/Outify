package cc.tomko.outify;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

public class TokenStore {

    private static final String PREFS_NAME = "tink_token_store";
    private static final String KEYSET_PREF = "tink_keyset";
    private static final String MASTER_KEY_URI = "android-keystore://tink_master_key";

    private final SharedPreferences sharedPreferences;
    private final Aead aead;

    public TokenStore(Context context) throws Exception {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize Tink AEAD
        KeysetHandle handle = new AndroidKeysetManager.Builder()
                .withSharedPref(context, PREFS_NAME, KEYSET_PREF)
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .getKeysetHandle();

        aead = handle.getPrimitive(Aead.class);
    }

    public void saveTokens(String accessToken, String refreshToken) throws Exception {
        byte[] encryptedAccess = aead.encrypt(accessToken.getBytes("UTF-8"), null);
        byte[] encryptedRefresh = aead.encrypt(refreshToken.getBytes("UTF-8"), null);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("access_token", Base64.encodeToString(encryptedAccess, Base64.NO_WRAP));
        editor.putString("refresh_token", Base64.encodeToString(encryptedRefresh, Base64.NO_WRAP));
        editor.apply();
    }

    public String[] loadTokens() throws Exception {
        String encAccessB64 = sharedPreferences.getString("access_token", null);
        String encRefreshB64 = sharedPreferences.getString("refresh_token", null);

        if (encAccessB64 == null || encRefreshB64 == null) return null;

        byte[] accessBytes = Base64.decode(encAccessB64, Base64.NO_WRAP);
        byte[] refreshBytes = Base64.decode(encRefreshB64, Base64.NO_WRAP);

        String accessToken = new String(aead.decrypt(accessBytes, null), "UTF-8");
        String refreshToken = new String(aead.decrypt(refreshBytes, null), "UTF-8");

        return new String[]{accessToken, refreshToken};
    }

    public void clearTokens() {
        sharedPreferences.edit().clear().apply();
    }
}

