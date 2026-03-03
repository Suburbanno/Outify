package cc.tomko.outify.core

import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpClient @Inject constructor() {
    external fun search(query: String, type: String, offset: Int = -1, pages: Int = -1): Array<String>

    external fun getUserCollection(query: String? = null): String

    /**
     * Adds given uris to users library
     */
    external fun saveItems(uris: Array<String>): Boolean

    /**
     * Retrieves the metadata for singular track by its ID
     */
    external fun getTrackData(id: String): String

    external fun getRootlist(): Array<String>

    /**
     * Returns JSON of `total` and `mediaItems` - containing object of `uri` holding URI to the radio playlist
     */
    external fun getRadioForTrack(trackUri: String): String
}

@Serializable
data class RadioResult(
    val total: Int,
    val mediaItems: List<RadioMediaItem>,
)

@Serializable
data class RadioMediaItem(
    val uri: String
)