package cc.tomko.outify.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpClient @Inject constructor() {
    external fun search(query: String, type: String, offset: Int = -1, pages: Int = -1): Array<String>

    external fun getUserCollection(query: String? = null): String

    /**
     * Retrieves the metadata for singular track by its ID
     */
    external fun getTrackData(id: String): String

    external fun getRootlist(): Array<String>
}