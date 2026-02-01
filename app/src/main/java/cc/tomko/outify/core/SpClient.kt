package cc.tomko.outify.core

class SpClient {
    external fun getLikedSongs(): String
    external fun search(query: String): String
}