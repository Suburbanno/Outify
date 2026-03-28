package cc.tomko.outify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


@UnstableApi
@Singleton
@AndroidEntryPoint
class MusicService @Inject constructor() : MediaSessionService() {
    @Inject
    lateinit var player: Player
    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    companion object {
        private const val CHANNEL_ID = "outify_playback"
        private const val notificationId: Int = 1001
    }

    private lateinit var mediaSession: MediaSession

    private var currentTrack: Track? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        val attrContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createAttributionContext("audioPlayback")
        } else {
            this
        }

        createNotificationChannel(attrContext)

        mediaSession = MediaSession.Builder(attrContext, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                        .add(SessionCommand(SessionCommand.COMMAND_CODE_SESSION_SET_RATING))
                        .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(availableCommands)
                        .build()
                }
            })
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build()
        )

        serviceScope.launch {
            playbackStateHolder.state.collect { state ->
                currentTrack = state.currentTrack

                if (currentTrack != null) {
                    postOrStartForegroundNotification(startInForeground = true)
                } else {
                    postOrStartForegroundNotification(startInForeground = false)
                }
            }
        }

        startForeground(notificationId, buildNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    private fun createNotificationChannel(attrContext: Context){
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Outify Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Playback controls"
            setShowBadge(false)
        }

        val nm = attrContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildContentIntent(): PendingIntent? {
        val appIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null

        return PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(): Notification {
        val track = currentTrack
        val metadata = mediaSession.player.mediaMetadata

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(androidx.media3.session.R.drawable.media3_icon_play)
            .setContentTitle(track?.name ?: metadata.title ?: "Not Playing")
            .setContentText(track?.artists?.joinToString { it.name } ?: metadata.artist ?: "Outify")
            .setSubText(track?.album?.name ?: metadata.albumTitle ?: "Unknown")
            .setLargeIcon(player.currentArtworkBitmap)
            .setContentIntent(buildContentIntent())
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        builder.setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))

        return builder.build()
    }

    private fun postOrStartForegroundNotification(startInForeground: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification()

        if (startInForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(notificationId, notification)
            }
        } else {
            notificationManager.notify(notificationId, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        postOrStartForegroundNotification(startInForeground = false)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        postOrStartForegroundNotification(startInForegroundRequired)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)

        mediaSession.release()
        player.handleRelease()
        serviceScope.cancel()
        super.onDestroy()
    }
}