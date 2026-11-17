package cc.stadapp.stad.service

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import cc.stadapp.stad.core.AppEntry

/**
 * Reads the phone's launchable apps. Needs the launcher `<queries>` entry in the manifest
 * (no `QUERY_ALL_PACKAGES`). Call from a background thread: PackageManager can be slow.
 *
 * TODO(slice1.1-work-profile): main profile only. In 1.2 the Accessibility service sees only the
 * package name, so blocking an app will also block its work-profile copy.
 */
class InstalledApps(context: Context) {

    private val pm: PackageManager = context.packageManager

    fun launchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryActivities(intent)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .map { info -> AppEntry(info.packageName, info.loadLabel(pm).toString()) }
    }

    /**
     * True unless the package is definitely gone. Uses getApplicationInfo rather than "is it in
     * the launcher list", so an app whose launcher entry is briefly disabled, or an archived app,
     * isn't dropped from the block list.
     */
    fun isInstalled(packageName: String): Boolean = try {
        pm.applicationInfoCompat(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

internal fun PackageManager.queryActivities(intent: Intent): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, 0)
    }

internal fun PackageManager.resolveActivityPackage(intent: Intent): String? {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        @Suppress("DEPRECATION")
        resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return info?.activityInfo?.packageName
}

internal fun PackageManager.applicationInfoCompat(packageName: String, flags: Int): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        getApplicationInfo(packageName, flags)
    }
