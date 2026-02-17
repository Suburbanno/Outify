package cc.tomko.outify.core.spirc

import android.util.Log
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.SessionCallback

class SpircController {
    private val session: Session = OutifyApplication.session

    fun start() {
        session.initializeSession(object : SessionCallback {
            override fun onInitialized() {
                initializeSpirc()
            }

            override fun onShutdown() {
                handleSessionShutdown()
            }

        })
    }

    private fun initializeSpirc(){
        Spirc.initializeSpirc(object : SpircInitializationCallback {
            override fun initialized() {
                activateAndTransfer()
            }

            override fun failed() {
                handleSpircFailure()
            }

        })
    }

    private fun activateAndTransfer(){
        if (!OutifyApplication.spirc.activate()) {
            Log.e("SpircController", "Failed to activate Spirc session!")
            return
        }

        if (!OutifyApplication.spirc.transfer()) {
            Log.e("SpircController", "Failed to transfer Spirc session!")
        }
    }

    private fun handleSessionShutdown() {
        Log.w("SpircController", "Session has shut down! Restarting..", );
    }

    private fun handleSpircFailure() {
        // Retry?
        // Tear down session?
    }
}