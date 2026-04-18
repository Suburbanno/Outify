package cc.tomko.outify.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat
import cc.tomko.outify.R

class OAuthService : Service() {

    companion object {
        const val CHANNEL_ID = "oauth_service_channel"
        const val NOTIFICATION_ID = 1001

        fun createNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Logging in...")
                .setContentText("Please complete authentication in the browser")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
        }

        fun start(context: Context) {
            context.startService(Intent(context, OAuthService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OAuthService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Authentication",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps authentication running"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification(this))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

object PendingAuthHelper {
    private const val PREFS_NAME = "auth_pending"
    private const val KEY_CODE = "code"
    private const val KEY_STATE = "state"

    fun savePendingAuth(context: Context, code: String, state: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CODE, code)
            .putString(KEY_STATE, state ?: "")
            .apply()
    }

    fun getPendingAuth(context: Context): Pair<String, String?>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_CODE, null) ?: return null
        val state = prefs.getString(KEY_STATE, null)?.takeIf { it.isNotEmpty() }
        return Pair(code, state)
    }

    fun clearPendingAuth(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}