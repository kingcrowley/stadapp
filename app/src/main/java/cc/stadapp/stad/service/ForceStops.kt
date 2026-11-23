package cc.stadapp.stad.service

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import cc.stadapp.stad.core.ForceStopRecord

/**
 * When the user last force-stopped Stad, from Android's process exit records (Android 11+). Only
 * records [ForceStopRecord] attributes to a force-stop count: not a Recents removal or an update.
 */
object ForceStops {
    fun lastAt(context: Context): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null // TODO(slice2.2-pre-r-force-stop): estimate only
        val am = context.getSystemService(ActivityManager::class.java) ?: return null
        // Android 14 files updates and component changes under their own reasons.
        val separatesUpdates = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val updatedAt = if (separatesUpdates) null else lastUpdateAt(context)
        return try {
            am.getHistoricalProcessExitReasons(context.packageName, 0, 0)
                .filter {
                    ForceStopRecord.isForceStop(
                        userRequested = it.reason == ApplicationExitInfo.REASON_USER_REQUESTED,
                        description = it.description,
                        exitAt = it.timestamp,
                        lastUpdateAt = updatedAt,
                        separatesUpdates = separatesUpdates,
                    )
                }
                .maxOfOrNull { it.timestamp }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun lastUpdateAt(context: Context): Long? = try {
        @Suppress("DEPRECATION") // The flags overload is enough here; the new one needs API 33.
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
