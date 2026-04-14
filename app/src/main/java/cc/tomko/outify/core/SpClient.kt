package cc.tomko.outify.core

import cc.tomko.outify.data.metadata.NativeError
import cc.tomko.outify.data.metadata.NativeErrorHandler
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpClient @Inject constructor() {
    companion object {
        private const val TAG = "SpClient"
    }

    /**
     * Returns currently logged-in user's username
     * Requires playback login
     */
    external fun username(): String
    external fun search(query: String, type: String, offset: Int = -1, pages: Int = -1): Array<String>

    external fun getUserCollection(query: String? = null): String

    /**
     * Adds given uris to users library
     */
    external fun saveItems(uris: Array<String>): Boolean

    /**
     * Removes given uris from users library
     */
    external fun deleteItems(uris: Array<String>): Boolean

    /**
     * Get the current user's top artists or tracks based on calculated affinity.
     * Default type = artists
     * Possible types: artists, tracks
     */
    external fun getUserTop(type: String? = null): String?

    /**
     * Gets the available devices to stream playback from.
     * In format of [cc.tomko.outify.core.model.DevicesResponse]
     */
    external fun getDevices(): String?

    /**
     * Transfers current playback device to the one with given ID
     */
    external fun transferPlaybackDevice(deviceId: String): Boolean

    /**
     * Check if user has authenticated with Spotify via OAuth.
     * Returns true if authenticated, false otherwise.
     */
    external fun isOAuthAuthenticated(): Boolean

    /**
     * Adds tracks in array into the playlist by given id
     */
    external fun addToPlaylist(playlist_id: String, track_uris: Array<String>): Boolean

    /**
     * Removes tracks in array from the playlist by given id
     */
    external fun deleteFromPlaylist(playlist_id: String, track_uris: Array<String>): Boolean

    /**
     * Retrieves the metadata for singular track by its ID
     */
    external fun getTrackData(id: String): String

    external fun getRootlist(): Array<String>

    /**
     * Returns JSON of `total` and `mediaItems` - containing object of `uri` holding URI to the radio playlist
     */
    external fun getRadioForTrack(trackUri: String): String

    /**
     * Returns lyrics for track id
     */
    external fun getLyrics(trackId: String): String

    /**
     * Starts the OAuth flow for SpotifyClient user authentication.
     * Returns the authorization URL to be opened in a webview.
     */
    external fun startOAuthFlow(): String

    /**
     * Completes the OAuth flow by exchanging the authorization code for tokens.
     * Call this after the user completes authorization.
     * Returns true on success, false on failure.
     */
    external fun completeOAuthFlow(code: String): Boolean

    fun checkAndHandleError(result: String, context: String = ""): String {
        if (result.startsWith("{")) {
            NativeErrorHandler.handleErrorJson(result, context)?.let {
                throw SpClientException(it.message, it)
            }
        }
        return result
    }
}

class SpClientException(
    message: String,
    val error: NativeError
) : Exception(message)

@Serializable
data class RadioResult(
    val total: Int,
    val mediaItems: List<RadioMediaItem>,
)

@Serializable
data class RadioMediaItem(
    val uri: String
)