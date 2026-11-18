package cc.stadapp.stad.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import cc.stadapp.stad.core.BlockListRules

/**
 * Packages that can never be blocked on this phone: the fixed floor in core (Stad, system UI,
 * the framework, the phone app, emergency apps) plus every home-screen app, every enabled
 * keyboard, the default dialer and SMS app, and Settings.
 * Each lookup is independent, so one that fails doesn't drop the others; but any failure marks
 * the result incomplete, and the picker stays read-only until a later refresh succeeds.
 * Needs the matching `<queries>` entries in the manifest.
 */
class ProtectedPackages(private val context: Context) {

    /** The protected set, and whether every lookup succeeded. */
    data class Result(val packages: Set<String>, val complete: Boolean)

    fun current(): Result {
        var complete = true
        val packages = buildSet {
            addAll(BlockListRules.ALWAYS_PROTECTED)
            add("com.android.settings")
            fun lookup(block: () -> List<String>) {
                try {
                    addAll(block())
                } catch (_: RuntimeException) {
                    complete = false
                }
            }
            lookup { homeApps() }
            lookup { keyboards() }
            lookup { listOfNotNull(defaultDialer()) }
            lookup { listOfNotNull(defaultSms()) }
            lookup { listOfNotNull(resolve(Intent(Settings.ACTION_SETTINGS))) }
            lookup { listOfNotNull(resolve(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))) }
        }
        return Result(packages, complete)
    }

    // Every home app, not just the default: with no default set, the "default" resolves to the
    // system chooser, and the user can switch launchers at any time.
    private fun homeApps(): List<String> =
        context.packageManager
            .queryActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            .map { it.activityInfo.packageName }

    // Every enabled keyboard, not just the current one: blocking a keyboard would break typing
    // everywhere, including in the apps that stay open.
    private fun keyboards(): List<String> =
        context.getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            .orEmpty()

    private fun defaultDialer(): String? =
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    private fun defaultSms(): String? = Telephony.Sms.getDefaultSmsPackage(context)

    private fun resolve(intent: Intent): String? = context.packageManager.resolveActivityPackage(intent)
}
