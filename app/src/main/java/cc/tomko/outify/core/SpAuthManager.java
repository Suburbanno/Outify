package cc.tomko.outify.core;

import android.util.Log;

import java.security.GeneralSecurityException;

import cc.tomko.outify.OutifyApplication;
import cc.tomko.outify.SecureStorage;
import cc.tomko.outify.core.auth.TokenResponseDto;

public class SpAuthManager {

    /**
     * Checks whether the user is already authenticated
     * @return <code>true</code> if OAuth already completed successfully
     */
    public boolean isAuthenticated() {
        final SecureStorage storage = OutifyApplication.secureStorage;

        if(storage == null) return false;
        return storage.contains(SecureStorage.Keys.ACCESS_TOKEN) && storage.contains(SecureStorage.Keys.REFRESH_TOKEN) &&
                storage.contains(SecureStorage.Keys.ACCESS_TOKEN_EXPIRATION);
    }

    /**
     * Gets the access token, if possible.
     * If access token is expired, automatically refreshes it, and stores the new refresh token.
     * @return access token if possible
     */
    public String getAccessToken(){
        // Check for stored token
        final SecureStorage storage = OutifyApplication.secureStorage;

        if(!storage.contains(SecureStorage.Keys.ACCESS_TOKEN) || !storage.contains(SecureStorage.Keys.REFRESH_TOKEN)){
            // TODO: Handle somehow
            return null;
        }

        // Checking expiration
        Long expiration;
        String oldRefresh, accessToken;
        try {
            expiration = storage.getObject(SecureStorage.Keys.ACCESS_TOKEN_EXPIRATION, Long.class);
            oldRefresh = storage.getString(SecureStorage.Keys.REFRESH_TOKEN);
            accessToken = storage.getString(SecureStorage.Keys.ACCESS_TOKEN);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        if(true || expiration == null || System.currentTimeMillis() > expiration) {
            Log.i("SpAuthManager", "Refreshing access token..");
            final TokenResponseDto result = refreshToken(accessToken,oldRefresh);

            // Refresh failed
            if(!result.isSuccess()){
                throw new RuntimeException("Token refresh failed with: " + result.error);
            }

            Log.i("SpAuthManager", "Access token refreshed!");
            try {
                storage.putString(SecureStorage.Keys.REFRESH_TOKEN, result.refreshToken);
                storage.putObject(SecureStorage.Keys.ACCESS_TOKEN_EXPIRATION, result.expiresAt);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return storage.getString(SecureStorage.Keys.ACCESS_TOKEN);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the OAuth Session
     * @return <code>true</code> if initialization was successful
     */
    public native boolean initialize(String client_id, String redirect_uri, String scopes);

    /**
     * Refreshes the OAuth token with given refresh_token;
     * @param refresh_token to refresh the token with.
     * @return {@link TokenResponseDto}
     */
    public native TokenResponseDto refreshToken(String access_token, String refresh_token);

    /**
    * Retrieves access, refresh token and token expiration.
     */
    public native TokenResponseDto getTokenData(String code, String state);

	/**
	 * Returns URL of Spotify authorization page
	 */
	public native String getAuthorizationURL();
}
