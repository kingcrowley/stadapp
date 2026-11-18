package cc.stadapp.stad.core

/**
 * Stops the block screen being started over and over for one burst of window events from the
 * same app, without stopping a real re-block a moment later.
 *
 * A launch for a package is skipped if the same package was blocked less than [windowMs] ago and
 * no other allowed app has come to the front since. Stad's own windows (the block screen) don't
 * count as "another app".
 */
class RelaunchGuard(private val windowMs: Long = 500) {

    private var lastPackage: String? = null
    private var lastAtMs: Long = 0

    /** Call when [packageName] should be blocked. True if the block screen should be started now. */
    fun shouldLaunch(packageName: String, nowMs: Long): Boolean {
        if (packageName == lastPackage && nowMs - lastAtMs in 0 until windowMs) return false
        lastPackage = packageName
        lastAtMs = nowMs
        return true
    }

    /** Call when an allowed app comes to the front. */
    fun onAllowed(packageName: String) {
        if (packageName != STAD_PACKAGE) lastPackage = null
    }
}
