package cc.tomko.outify.core

import android.util.Log
import cc.tomko.outify.core.spirc.Spirc

/**
 * Handles the librespot session
 */
class Session {
    val spClient: SpClient = SpClient()

    external fun initializeSession(callback: SessionCallback)

    external fun shutdown(): Boolean
}

interface SessionCallback {
    /**
     * Called when the session gets initialized
     */
    fun onInitialized()

    /**
     * Called when the session shutdowns
     */
    fun onShutdown()
}