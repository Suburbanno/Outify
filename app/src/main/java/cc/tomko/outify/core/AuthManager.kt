package cc.tomko.outify.core;

public class AuthManager {
    /**
     * Checks for cached credentials.
     * Performs raw filesystem check
     */
    public native boolean hasCachedCredentials();

    /**
    * Processes the OAuth Code and caches the credentials into storage if success
     */
    public native boolean handleOAuthCode(String code, String state);

	/**
	 * Returns URL of Spotify authorization page
	 */
	public native String getAuthorizationURL();
}
