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
import android.graphics.drawable.BitmapDrawable
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
import cc.tomko.outify.data.CoverSize
import cc.tomko.outify.data.Track
import cc.tomko.outify.data.getCover
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.playback.Player
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
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

    private var currentArtwork: Bitmap? = null
    private var currentArtworkUrl: String? = null

    private var currentTrack: Track? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

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

        serviceScope.launch {
            playbackStateHolder.state.collect { state ->
                val track = state.currentTrack
                val cover = track?.album?.getCover(CoverSize.LARGE)
                val artworkUri = cover?.uri

                if (artworkUri != null) {
                    val url = ALBUM_COVER_URL + artworkUri
                    loadArtworkFromUrl(url) {}
                } else {
                    postOrStartForegroundNotification(startInForeground = false)
                }

                currentTrack = track
            }
        }

        startForeground(notificationId, buildNotification())
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
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(androidx.media3.session.R.drawable.media3_icon_play)
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

    private fun loadArtworkFromUrl(url: String, callback: (Bitmap?) -> Unit) {
        if (url != currentArtworkUrl) {
            currentArtworkUrl = url

            serviceScope.launch {
                val loader = this@MusicService.applicationContext.imageLoader

                val request = ImageRequest.Builder(this@MusicService)
                    .size(coil3.size.Size.ORIGINAL)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)

                val bitmap = result.image?.toBitmap()

                currentArtwork = bitmap
                postOrStartForegroundNotification(startInForeground = false)
                callback(bitmap)
            }
        } else {
            callback(currentArtwork)
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