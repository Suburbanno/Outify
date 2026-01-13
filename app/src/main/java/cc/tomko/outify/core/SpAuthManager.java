package cc.tomko.outify.core;

public class SpAuthManager {
    /**
     * Initializes the OAuth Session
     * @return <code>true</code> if initialization was successful
     */
    public native boolean initialize(String client_id, String redirect_uri, String scopes);

	/**
	 *  Returns the access token in plain text
	 */
	public native String getAccessToken(String code, String state);

	/**
	 * Returns URL of Spotify authorization page
	 */
	public native String getAuthorizationURL();
}
