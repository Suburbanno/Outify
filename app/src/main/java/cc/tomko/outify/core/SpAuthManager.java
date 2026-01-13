package cc.tomko.outify.core;

import cc.tomko.outify.OutifyApplication;

public class SpAuthManager {

    /**
     * Checks whether the user is already authenticated
     * @return <code>true</code> if OAuth already completed successfully
     */
    public boolean isAuthenticated() {
        String[] tokens = null;
        try {
            tokens = OutifyApplication.tokenStore.loadTokens();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(tokens == null){
            return false;
        }

        return tokens[0] != null && tokens[1] != null && !tokens[0].isEmpty() && !tokens[1].isEmpty();
    }

    /**
     * Initializes the OAuth Session
     * @return <code>true</code> if initialization was successful
     */
    public native boolean initialize(String client_id, String redirect_uri, String scopes);

	/**
	 *  Returns the access & refresh token in plain text after completing the OAuth cycle.
	 */
	public native String[] getTokenPair(String code, String state);

	/**
	 * Returns URL of Spotify authorization page
	 */
	public native String getAuthorizationURL();
}
