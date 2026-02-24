package cc.tomko.outify.core.spirc

import android.util.Log
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.SessionCallback
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.model.PlaybackState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpircController @Inject constructor(
    private val session: Session,
    private val spirc: SpircWrapper,
    private val playbackStateHolder: PlaybackStateHolder
) {
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
                Spirc.bufferCallback(object : SpircBufferCallback {
                    override fun started() {
                        playbackStateHolder.setBuffering(true)
                    }

                    override fun stopped() {
                        playbackStateHolder.setBuffering(false)
                    }

                })

                activateAndTransfer()
            }

            override fun failed() {
                handleSpircFailure()
            }

        })
    }

    private fun activateAndTransfer(){
        if (!spirc.activate()) {
            Log.e("SpircController", "Failed to activate Spirc session!")
            return
        }

        if (!spirc.transfer()) {
            Log.e("SpircController", "Failed to transfer Spirc session!")
        }

        spirc.isUsable = true
    }

    private fun handleSessionShutdown() {
        Log.w("SpircController", "Session has shut down! Restarting..", );
        spirc.isUsable = false
    }

    private fun handleSpircFailure() {
        // Retry?
        // Tear down session?
        spirc.isUsable = false
    }
}