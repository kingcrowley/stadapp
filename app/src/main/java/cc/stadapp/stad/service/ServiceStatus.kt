package cc.stadapp.stad.service

import android.content.Context
import android.provider.Settings
import cc.stadapp.stad.core.AccessibilityStatus

/** Whether Stad's Accessibility service is turned on in system settings. */
object ServiceStatus {
    fun isOn(context: Context): Boolean = AccessibilityStatus.isEnabled(
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
        context.packageName,
        BlockerAccessibilityService::class.java.name,
    )
}
