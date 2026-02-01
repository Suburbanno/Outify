package cc.tomko.outify.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlin.math.max

private const val TAG = "AudioPlayer"

enum class PcmFormat {
    S16,
}

/**
 * Plays the received PCM audio using modern AudioAttributes/AudioFormat API.
 */
class AudioPlayer(context: Context) {
    @Volatile
    private var track: AudioTrack? = null

    private var currentSampleRate = -1
    private var currentChannels = -1
    private var currentFormat: PcmFormat? = null

    private val audioContext: Context =
        if (Build.VERSION.SDK_INT >= 31) {
            context.createAttributionContext("audioPlayback")
        } else {
            context
        }

    /**
     * Feed raw PCM bytes into the player.
     */
    @Synchronized
    fun onPcm(data: ByteArray, sampleRate: Int, channels: Int, format: PcmFormat) {
        if (!ensureAudioTrack(sampleRate, channels, format)) {
            // unsupported format or allocation failed
            return
        }

        writePcm(data, format)
    }

    @Synchronized
    private fun ensureAudioTrack(sampleRate: Int, channels: Int, format: PcmFormat): Boolean {
        val existing = track
        if (existing != null
            && sampleRate == currentSampleRate
            && channels == currentChannels
            && format == currentFormat
            && existing.state == AudioTrack.STATE_INITIALIZED
        ) {
            return true
        }

        // Otherwise recreate
        releaseAudioTrack()

        val channelMask = when (channels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> {
                // fallback to stereo for unknown channel counts
                Log.w(TAG, "Unsupported channel count $channels, falling back to stereo")
                AudioFormat.CHANNEL_OUT_STEREO
            }
        }

        val encoding = when (format) {
            PcmFormat.S16 -> AudioFormat.ENCODING_PCM_16BIT
        }

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid min buffer size: $minBufferSize")
            return false
        }

        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val frameSize = bytesPerSample * max(1, channels)
        val bufferSize = max(minBufferSize, frameSize * 1024)

        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val formatBuilder = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(encoding)
                .setChannelMask(channelMask)
                .build()

            val newTrack = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(formatBuilder)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (newTrack.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioTrack: state=${newTrack.state}")
                newTrack.release()
                return false
            }

            newTrack.play()

            track = newTrack
            currentSampleRate = sampleRate
            currentChannels = channels
            currentFormat = format

            Log.d(TAG, "AudioTrack created: sampleRate=$sampleRate, channels=$channels, encoding=$encoding, buffer=$bufferSize")
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "Exception while creating AudioTrack", t)
            return false
        }
    }

    @Synchronized
    private fun writePcm(data: ByteArray, format: PcmFormat) {
        val t = track ?: run {
            Log.w(TAG, "writePcm called with no audio track")
            return
        }

        when (format) {
            PcmFormat.S16 -> {
                try {
                    var offset = 0
                    var remaining = data.size
                    while (remaining > 0) {
                        val written = t.write(data, offset, remaining)
                        if (written < 0) {
                            Log.e(TAG, "AudioTrack.write returned error: $written")
                            break
                        }
                        offset += written
                        remaining -= written
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "AudioTrack write failed", e)
                }
            }
        }
    }

    @Synchronized
    fun releaseAudioTrack() {
        val t = track ?: return
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    t.stop()
                } catch (ignored: IllegalStateException) {
                    // ignore - may happen if already stopped
                }
            } else if (t.playState == AudioTrack.PLAYSTATE_PAUSED) {
                try {
                    t.stop()
                } catch (ignored: IllegalStateException) {
                }
            }
        } catch (ignored: Exception) {
        } finally {
            try {
                t.release()
            } catch (ignored: Exception) {
            }
            track = null
            currentSampleRate = -1
            currentChannels = -1
            currentFormat = null
        }
    }

    @Synchronized
    fun pause() {
        track?.let {
            try {
                it.pause()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "pause() failed", e)
            }
        }
    }

    @Synchronized
    fun flush() {
        track?.let {
            try {
                it.flush()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "flush() failed", e)
            }
        }
    }
}
