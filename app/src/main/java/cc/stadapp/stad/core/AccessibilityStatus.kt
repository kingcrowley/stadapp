package cc.stadapp.stad.core

/** Reads Android's list of enabled Accessibility services. Pure, so it is JVM-tested. */
object AccessibilityStatus {

    /**
     * Whether the service [packageName]/[className] appears in [enabledServices], the
     * colon-separated `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` value. Entries may use the
     * short form (`pkg/.Cls`) or the full form (`pkg/pkg.Cls`); both match.
     */
    fun isEnabled(enabledServices: String?, packageName: String, className: String): Boolean =
        enabledServices.orEmpty()
            .split(':')
            .any { entry ->
                val pkg = entry.substringBefore('/', missingDelimiterValue = "").trim()
                val cls = entry.substringAfter('/', missingDelimiterValue = "").trim()
                val fullCls = if (cls.startsWith('.')) pkg + cls else cls
                pkg == packageName && fullCls == className
            }
}
