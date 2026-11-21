package cc.stadapp.stad.service

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/** When the user last force-stopped Stad, from Android's process exit records (Android 11+). */
object ForceStops {
    fun lastAt(context: Context): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null // TODO(slice2.2-pre-r-force-stop): estimate only
        val am = context.getSystemService(ActivityManager::class.java) ?: return null
        return try {
            am.getHistoricalProcessExitReasons(context.packageName, 0, 0)
                .filter { it.reason == ApplicationExitInfo.REASON_USER_REQUESTED }
                .maxOfOrNull { it.timestamp }
        } catch (_: RuntimeException) {
            null
        }
    }
}
