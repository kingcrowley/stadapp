package cc.stadapp.stad.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import cc.stadapp.stad.core.BlockListRules

/**
 * Packages that can never be blocked on this phone: the fixed floor in core (Stad, system UI,
 * the framework) plus every home-screen app, the default dialer and SMS app, and Settings.
 * Each lookup is independent, so one that fails or returns null doesn't drop the others.
 * Needs the matching `<queries>` entries in the manifest.
 *
 * TODO(slice1.2-emergency-apps): consider the OEM emergency/safety app as well.
 */
class ProtectedPackages(private val context: Context) {

    fun current(): Set<String> = buildSet {
        addAll(BlockListRules.ALWAYS_PROTECTED)
        add("com.android.settings")
        addAll(safe { homeApps() })
        addAll(safe { listOfNotNull(defaultDialer()) })
        addAll(safe { listOfNotNull(defaultSms()) })
        addAll(safe { listOfNotNull(resolve(Intent(Settings.ACTION_SETTINGS))) })
        addAll(safe { listOfNotNull(resolve(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))) })
    }

    // Every home app, not just the default: with no default set, the "default" resolves to the
    // system chooser, and the user can switch launchers at any time.
    private fun homeApps(): List<String> =
        context.packageManager
            .queryActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            .map { it.activityInfo.packageName }

    private fun defaultDialer(): String? =
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    private fun defaultSms(): String? = Telephony.Sms.getDefaultSmsPackage(context)

    private fun resolve(intent: Intent): String? = context.packageManager.resolveActivityPackage(intent)

    private inline fun safe(block: () -> List<String>): List<String> =
        try { block() } catch (_: RuntimeException) { emptyList() }
}
