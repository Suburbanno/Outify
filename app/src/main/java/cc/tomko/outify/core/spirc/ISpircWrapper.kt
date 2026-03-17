package cc.tomko.outify.core.spirc

interface ISpircWrapper {
    fun startRadio(trackUri: String, shuffle: Boolean = true): Boolean
    fun load(context: String? = null, playingTrackUri: String? = null): Boolean
    fun localLoad(uri: String): Boolean
    fun shuffle(enabled: Boolean): Boolean
    fun repeat(enabled: Boolean): Boolean
    fun shuffleLoad(uri: String? = null): Boolean
    fun addToQueue(uri: String?): Boolean
    fun setQueue(uris: Array<String>): Boolean
    fun activate(): Boolean
    fun transfer(): Boolean
    suspend fun seekTo(positionMs: Long): Boolean
    fun playerPlay(): Boolean
    fun playerPause(): Boolean
    fun playerPlayPause(): Boolean
    fun playerNext(): Boolean
    fun playerPrevious(): Boolean
    fun previousTracks(): String
    fun nextTracks(): String
}