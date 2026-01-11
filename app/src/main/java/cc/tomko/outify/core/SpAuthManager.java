package cc.tomko.outify.core;

public class SpAuthManager {
	/**
	 *  Returns the access token in plain text
	 */
	public native String oauthGetAccessToken();

	/**
	 * Returns URL of Spotify authorization page
	 */
	public native String getAuthURL();
}
