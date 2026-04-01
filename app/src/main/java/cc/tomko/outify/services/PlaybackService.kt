package cc.tomko.outify.services

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Binder
import android.util.Log
import android.widget.Toast
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import cc.tomko.outify.MainActivity
import cc.tomko.outify.MediaSessionConstants
import cc.tomko.outify.R
import cc.tomko.outify.data.metadata.TrackMetadataHelper
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.Player
import cc.tomko.outify.ui.repository.SettingsRepository
import cc.tomko.outify.utils.CoilBitmapLoader
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Singleton


@UnstableApi
@Singleton
@AndroidEntryPoint
class PlaybackService : MediaLibraryService(),
    androidx.media3.common.Player.Listener
{
    companion object {
        const val ROOT = "root"
        const val TRACK = "track"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val SEARCH = "search"

        const val NOTIFICATION_ID = 4894
        const val CHANNEL_ID = "outify_channel_01"
        const val CHANNEL_NAME = "Media Playback"

        val TAG = PlaybackService::class.simpleName.toString()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media playback controls"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private val offloadScope = CoroutineScope(Dispatchers.IO)

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var trackDatabase: TrackMetadataHelper

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val binder = MusicBinder()

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onCreate() {
        Log.i(TAG, "Starting PlaybackService")
        super.onCreate()

        createNotificationChannel()

        mediaLibrarySessionCallback.apply {
            service = this@PlaybackService
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, mediaLibrarySessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setBitmapLoader(CoilBitmapLoader(
                scope,
                context = this,
            ))
            .build()

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val browserFuture = MediaBrowser.Builder(this, sessionToken).buildAsync()
        browserFuture.addListener({ browserFuture.get() }, MoreExecutors.directExecutor())

        scope.launch {
            playbackStateHolder.state
                .map { it.currentTrack }
                .distinctUntilChanged()
                .collect { track ->
                    println("new track: ${track?.uri}")
                    updateNotification()
                }
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this@PlaybackService,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.app_name
            ).apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
            }
        )
    }

    fun toggleLike() {
        Log.i(TAG,"toggle like")
    }

    fun toggleStartRadio(){
        Log.i(TAG,"Starting radio")
        val item = player.currentMediaItem ?: return
        val id = item.mediaId
        println(id)
    }

    fun updateNotification() {
        mediaLibrarySession ?: return
        val track = playbackStateHolder.state.value.currentTrack

        mediaLibrarySession!!.setCustomLayout(
            listOf(
                CommandButton.Builder(when (player.repeatMode) {
                    REPEAT_MODE_OFF -> CommandButton.ICON_SHUFFLE_OFF
                    REPEAT_MODE_ONE -> CommandButton.ICON_SHUFFLE_ON
                    REPEAT_MODE_ALL -> CommandButton.ICON_SHUFFLE_STAR
                    else -> throw IllegalStateException()
                })
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            }
                        )
                    )
                    .setSessionCommand(MediaSessionConstants.CommandToggleRepeatMode)
                    .build(),
                CommandButton.Builder(CommandButton.ICON_RADIO)
                    .setDisplayName(getString(R.string.start_radio))
                    .setSessionCommand(MediaSessionConstants.CommandToggleStartRadio)
                    .setEnabled(track != null)
                    .build()
            )
        )
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Toast.makeText(
            this@PlaybackService,
            "plr: ${error.message} (${error.errorCode}): ${error.cause?.message ?: ""}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPlaybackStateChanged(@androidx.media3.common.Player.State playbackState: Int) {
        if(playbackState == STATE_IDLE) {
            Log.i(TAG, "Playback idling")
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        offloadScope.launch {
            settings.setRepeat(repeatMode != REPEAT_MODE_OFF)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        player.shuffleModeEnabled = shuffleModeEnabled
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // TODO: Add keepalive preference
        if(player.isPlaying) {
            super.onUpdateNotification(session, startInForegroundRequired)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "Terminating PlaybackService")

        mediaLibrarySession?.run {
            player.stop()
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()

        Log.i(TAG, "Terminated PlaybackService")
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    inner class MusicBinder : Binder() {
        val service: PlaybackService
            get() = this@PlaybackService
    }
}
