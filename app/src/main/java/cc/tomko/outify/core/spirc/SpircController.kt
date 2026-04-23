package cc.tomko.outify.core.spirc

import android.util.Log
import cc.tomko.outify.core.Session
import cc.tomko.outify.core.SessionCallback
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.model.OutifyUri
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.data.repository.SettingsRepository
import cc.tomko.outify.playback.model.getSpeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
                spirc.scope.launch {
                    initializeSpirc()
                }
            }

            override fun onShutdown() {
                handleSessionShutdown()
            }

            override fun onAutoRestart() {
                handleSessionAutoRestart()
            }
        })
    }

    fun restart() {
        Log.w("SpircController", "Restarting session...")
        session.shutdown()
        spirc.shutdown()
        start()
    }

    private suspend fun initializeSpirc(){
        val gapless = settingsRepository.gaplessPlayback.first()
        val normalise = settingsRepository.normalizePlayback.first()
        val bitrate = settingsRepository.bitrate.first()

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

                Spirc.deviceCallback(object : SpircDeviceCallback {
                    override fun becameActive() {
                        println("we are active")
                        playbackStateHolder.setActiveDevice(true)
                    }

                    override fun becameInactive() {
                        println("we are inactive")
                        playbackStateHolder.setActiveDevice(false)
                    }
                })

                spirc.scope.launch {
                    activateAndTransfer()
                    restoreLastPlayback()
                }
            }

            override fun failed() {
                handleSpircFailure()
            }

        }, gapless, normalise, bitrate.getSpeed())
    }

    private suspend fun restoreLastPlayback() {
        val lastContextUri = settingsRepository.lastContextUri.first() ?: return
        val lastTrackUri = settingsRepository.lastTrackUri.first()
        val lastPositionMs = settingsRepository.lastPositionMs.first()

        if (lastContextUri.isNullOrBlank()) return

        Log.i("SpircController","Restoring last playback: $lastContextUri @ ${lastTrackUri ?: "first"}")

        if (lastTrackUri != null) {
            spirc.load(OutifyUri.fromUriString(lastContextUri), OutifyUri.fromUriString(lastTrackUri))
        } else {
            spirc.load(OutifyUri.fromUriString(lastContextUri), null)
        }
        spirc.playerPause()

        if (lastPositionMs != null && lastPositionMs > 0) {
            spirc.seekTo(lastPositionMs)
        }
    }

    private fun activateAndTransfer(){
        if (!spirc.activate()) {
            Log.e("SpircController", "Failed to activate Spirc session!")
            return
        }

        if (!spirc.transfer()) {
            Log.e("SpircController", "Failed to transfer Spirc session!")
        }

        playbackStateHolder.setActiveDevice(true)

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