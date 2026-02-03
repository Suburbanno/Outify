package cc.tomko.outify.core

class SpClient {
    external fun search(query: String, pageOffset: Int, pages: Int): String

    external fun getLikedSongs(pageOffset: Int, pages: Int): String

    /**
     * Retrieves the metadata for singular track by its ID
     */
    external fun getTrackData(id: String): String
}