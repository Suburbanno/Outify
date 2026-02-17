package cc.tomko.outify.core

import android.util.Log
import cc.tomko.outify.core.spirc.Spirc
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the librespot session
 */
@Singleton
class Session @Inject constructor() {
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