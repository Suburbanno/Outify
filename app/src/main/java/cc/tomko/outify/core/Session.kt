package cc.tomko.outify.core

import android.util.Log
import cc.tomko.outify.core.spirc.Spirc

/**
 * Handles the librespot session
 */
class Session {
    val spClient: SpClient = SpClient()

    external fun initializeSession(callback: Runnable)

    external fun shutdown(): Boolean

    companion object {
        @JvmStatic
        fun onSessionConnected(){
            Log.w("Session", "onSessionConnected: a", )
            Spirc.initializeSpirc()
        }

        @JvmStatic
        fun onSessionShutdown(){
            Log.w("Session", "onSessionShutdown: a", )
        }
    }
}