package cc.stadapp.stad.core

/** One launchable app as the picker sees it. [label] falls back to [packageName] when blank. */
data class AppEntry(val packageName: String, val label: String) {
    val displayName: String get() = label.ifBlank { packageName }
}
