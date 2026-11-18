package cc.stadapp.stad.core

import java.text.Normalizer

/**
 * Pure rules for the block list. No Android imports: everything here is JVM-unit-tested.
 * Android code gathers facts (installed apps, protected packages, lock state) and calls these.
 */
const val STAD_PACKAGE = "cc.stadapp.stad"

object BlockListRules {

    /**
     * The block list is read-only while locked (otherwise "unlock" would just be "edit the list"),
     * and while Stad couldn't confirm which apps are protected (otherwise a launcher or SMS app
     * missed by a failed lookup could be picked).
     */
    fun canEdit(lockState: LockState, protectionComplete: Boolean = true): Boolean =
        !lockState.isLocked && protectionComplete

    fun isBlockable(packageName: String, protectedPackages: Set<String>): Boolean =
        packageName.isNotBlank() && packageName !in protectedPackages

    /**
     * Sets [packageName] blocked or unblocked. Returns [list] unchanged when locked, or when
     * trying to block a protected package (unblocking is always allowed while unlocked, so a
     * package that became protected can still be cleared).
     */
    fun setBlocked(
        list: BlockList,
        packageName: String,
        blocked: Boolean,
        lockState: LockState,
        protectedPackages: Set<String>,
        protectionComplete: Boolean = true,
    ): BlockList {
        if (!canEdit(lockState, protectionComplete)) return list
        if (blocked && !isBlockable(packageName, protectedPackages)) return list
        return list.with(packageName, blocked)
    }

    /**
     * Packages that can never be blocked, whatever the device reports: Stad itself, the system UI
     * and the Android framework. Device-specific ones (launchers, dialer, SMS, Settings) are added
     * by the Android layer.
     */
    val ALWAYS_PROTECTED: Set<String> = setOf(STAD_PACKAGE, "com.android.systemui", "android")

    /**
     * Drops packages that are confirmed uninstalled, and nothing else. Protection is applied when
     * reading ([effective]), never written to storage: otherwise making a blocked app the default
     * launcher while locked would silently delete it from the list.
     */
    fun sanitize(saved: BlockList, isInstalled: (String) -> Boolean): BlockList =
        BlockList(saved.packages.filterTo(mutableSetOf(), isInstalled))

    /**
     * Whether to write the sanitized list back. Never while locked (the list is read-only),
     * never when the device returned no installed apps at all (a failed query must not wipe the
     * list), and only if something actually changed.
     */
    fun shouldPersistSanitized(
        saved: BlockList,
        sanitized: BlockList,
        lockState: LockState,
        installedAppCount: Int,
    ): Boolean = canEdit(lockState) && installedAppCount > 0 && sanitized != saved

    /** What is actually blocked: the stored list minus anything currently protected. */
    fun effective(list: BlockList, protectedPackages: Set<String>): BlockList =
        BlockList(list.packages.filterTo(mutableSetOf()) { isBlockable(it, protectedPackages) })

    /** The "N apps blocked" figure on Home. */
    fun blockedCount(list: BlockList, protectedPackages: Set<String>): Int =
        effective(list, protectedPackages).size

    /**
     * Rows for the picker: filtered by [query] (case- and accent-insensitive, matching the app
     * name or package name), sorted by name with ties broken by package name.
     */
    fun pickerRows(
        apps: List<AppEntry>,
        list: BlockList,
        protectedPackages: Set<String>,
        lockState: LockState,
        query: String = "",
        protectionComplete: Boolean = true,
    ): List<PickerRow> {
        val needle = fold(query.trim())
        val editable = canEdit(lockState, protectionComplete)
        return apps
            .asSequence()
            .distinctBy { it.packageName }
            .filter { needle.isEmpty() || needle in fold(it.displayName) || needle in fold(it.packageName) }
            .sortedWith(compareBy<AppEntry> { fold(it.displayName) }.thenBy { it.packageName })
            .map { app ->
                val isProtected = !isBlockable(app.packageName, protectedPackages)
                PickerRow(
                    app = app,
                    checked = app.packageName in list && !isProtected,
                    protected = isProtected,
                    enabled = editable && !isProtected,
                )
            }
            .toList()
    }

    private val combiningMarks = Regex("\\p{Mn}+")

    /** Lower-cases and strips accents, so "cafe" matches "Café". */
    internal fun fold(text: String): String =
        combiningMarks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()
}
