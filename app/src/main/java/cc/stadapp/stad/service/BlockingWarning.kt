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
import java.util.concurrent.atomic.AtomicInteger

/**
 * The "Stad is locked, but blocking is off" notification. One fixed id, so a repeat replaces it.
 * Cancelled when the service connects and whenever Home is shown (Home carries the same warning).
 */
object BlockingWarning {
    private const val CHANNEL_ID = "blocking_warnings"
    private const val NOTIFICATION_ID = 1

    // Bumped by every cancel, so a check that started before Home or the service cancelled the
    // warning can't post it again afterwards. Same process for the receiver, Home and the service.
    private val cancels = AtomicInteger()

    /** A token for [post]: the warning is posted only if nothing cancelled it since. */
    fun token(): Int = cancels.get()

    /**
     * Posts the warning, unless it was cancelled since [token] was taken, blocking is back on, or
     * Stad may not post notifications.
     */
    fun post(context: Context, token: Int) {
        if (cancels.get() != token || ServiceStatus.isOn(context)) return
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
        try {
            // Last check right before posting; a cancel after this removes it as normal.
            if (cancels.get() != token) return
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check and here: Home still warns.
        }
    }

    fun cancel(context: Context) {
        cancels.incrementAndGet()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
