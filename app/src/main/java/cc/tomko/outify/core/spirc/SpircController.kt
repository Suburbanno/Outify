package cc.tomko.outify.core.spirc

import android.util.Log
import androidx.compose.runtime.collectAsState
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.SessionCallback
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpircController @Inject constructor(
    private val session: Session,
    private val spirc: SpircWrapper,
    private val playbackStateHolder: PlaybackStateHolder,
    private val settingsRepository: SettingsRepository,
) {
    fun start() {
        session.initializeSession(object : SessionCallback {
            override fun onInitialized() {
                initializeSpirc()
            }

            override fun onShutdown() {
                handleSessionShutdown()
            }

            override fun onAutoRestart() {
                handleSessionAutoRestart()
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

        spirc.scope.launch {
            val shuffle = settingsRepository.shuffleEnabled.first()
            val repeat = settingsRepository.repeatEnabled.first()

            spirc.shuffle(shuffle)
            spirc.repeat(repeat)
        }
    }

    private fun handleSessionShutdown() {
        Log.w("SpircController", "Session has shut down! Restarting..", );
        spirc.isUsable = false
    }

    private fun handleSessionAutoRestart(){
        // TODO: show some toast
    }

    private fun handleSpircFailure() {
        // Retry?
        // Tear down session?
        spirc.isUsable = false
    }
}