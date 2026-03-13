package cc.tomko.outify.core

import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class AuthManager @Inject constructor() {
    /**
     * Checks for cached credentials.
     * Performs raw filesystem check
     */
    external fun hasCachedCredentials(): Boolean

    /**
     * Processes the OAuth Code and caches the credentials into storage if success
     */
    external fun handleOAuthCode(code: String, state: String? = ""): Boolean

    /**
     * Returns URL of Spotify authorization page
     */
    external fun getAuthorizationURL(): String
}
