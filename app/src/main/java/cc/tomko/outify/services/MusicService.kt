package cc.tomko.outify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.ui.PlayerNotificationManager
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.R
import cc.tomko.outify.playback.Player
import cc.tomko.outify.playback.media3.MediaNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.URL


@UnstableApi
class MusicService : MediaSessionService() {
    private val CHANNEL_ID = "outify_playback"
    private val notificationId: Int
        get() = CHANNEL_ID.hashCode()

    private lateinit var mediaSession: MediaSession

    private var currentArtwork: Bitmap? = null
    private var currentArtworkUrl: URL? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val player = (this.applicationContext as OutifyApplication).player

        mediaSession = MediaSession.Builder(this, player)
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
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    private fun createNotificationChannel(){
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Outify Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Playback controls"
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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
        val currentTrack = OutifyApplication.playbackStateHolder.state.value.currentTrack
        val artworkUrl = ALBUM_COVER_URL + currentTrack?.album?.covers?.first()?.uri

        loadArtworkFromUrl(URL(artworkUrl)) { bitmap ->
            currentArtwork = bitmap
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(androidx.media3.session.R.drawable.media3_icon_circular_play)
            .setContentTitle(currentTrack?.name ?: "Currently not playing")
            .setContentText(currentTrack?.artists?.joinToString { it.name } ?: "Outify")
            .setSubText(currentTrack?.album?.name ?: "Unknown")
            .setLargeIcon(currentArtwork)
            .setContentIntent(buildContentIntent())
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        builder.setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))

        return builder.build()
    }

    private fun postOrStartForegroundNotification(startInForeground: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    private fun loadArtworkFromUrl(url: URL, callback: (Bitmap?) -> Unit) {
        if(url != currentArtworkUrl) {
            currentArtworkUrl = url
            serviceScope.launch {
                try {
                    val inputStream = url.openConnection().getInputStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    callback(bitmap)
                } catch(e: Exception) {
                    callback(null)
                }
            }
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
        val player = (this.applicationContext as OutifyApplication).player
        stopForeground(STOP_FOREGROUND_REMOVE)

        mediaSession.release()
        player.handleRelease()
        serviceScope.cancel()
        super.onDestroy()
    }
}