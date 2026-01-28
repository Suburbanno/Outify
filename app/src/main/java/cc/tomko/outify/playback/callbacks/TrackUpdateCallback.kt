package cc.tomko.outify.playback.callbacks

import cc.tomko.outify.data.Track
import cc.tomko.outify.data.native.NativeTrack

/**
 * Called from Rust when the track updates.
 */
interface TrackUpdateCallback {
    /**
     * Called when the track changes completely
     */
    fun onTrackPlaying(track: NativeTrack)
}