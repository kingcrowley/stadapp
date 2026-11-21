package cc.stadapp.stad.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cc.stadapp.stad.MainActivity
import cc.stadapp.stad.R

/**
 * The "Stad is locked, but blocking is off" notification. One fixed id, so a repeat replaces it.
 * Cancelled when the service connects and whenever Home is shown (Home carries the same warning).
 */
object BlockingWarning {
    private const val CHANNEL_ID = "blocking_warnings"
    private const val NOTIFICATION_ID = 1

    // Bumped by every cancel, so a check that started before Home or the service cancelled the
    // warning can't post it again afterwards. Same process for the receiver, Home and the service.
    // The final check plus notify, and the bump plus cancel, each run under one lock, so a cancel
    // can't slip between them.
    private val lock = Any()
    private var cancels = 0

    /** A token for [post]: the warning is posted only if nothing cancelled it since. */
    fun token(): Int = synchronized(lock) { cancels }

    /**
     * Posts the warning, unless it was cancelled since [token] was taken, blocking is back on, or
     * Stad may not post notifications.
     */
    fun post(context: Context, token: Int) {
        if (token() != token || ServiceStatus.isOn(context)) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.warning_channel_name))
                .build(),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.warning_title))
            .setContentText(context.getString(R.string.warning_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        synchronized(lock) {
            if (cancels != token) return
            try {
                manager.notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Permission withdrawn between the check and here: Home still warns.
            }
        }
    }

    fun cancel(context: Context) {
        synchronized(lock) {
            cancels++
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
