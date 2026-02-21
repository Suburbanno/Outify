package cc.tomko.outify.profile

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfile @Inject constructor() {
    /**
     * Retrieves the users profile as a string
     */
    external fun getUserProfile(username: String? = null): String
}