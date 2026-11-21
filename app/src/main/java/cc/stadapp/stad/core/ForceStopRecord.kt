package cc.stadapp.stad.core

import kotlin.math.abs

/**
 * Whether one of Android's process exit records (Android 11+) was a force-stop. Android files
 * several kinds of exit under "user requested": a force-stop, removal from Recents, and, before
 * Android 14, a package update or component change. Only a force-stop turns blocking off, so the
 * others must not start an exact off period.
 */
object ForceStopRecord {

    /** An exit this close to the app's last update time is the update restarting it. */
    const val UPDATE_MARGIN_MS = 60_000L

    /**
     * @param userRequested the record's reason is `REASON_USER_REQUESTED`.
     * @param description the record's description. AOSP writes "remove task" for a Recents removal.
     * @param lastUpdateAt when Stad was last installed or updated; needed only where Android
     *   doesn't separate updates ([separatesUpdates] false, before Android 14).
     */
    fun isForceStop(
        userRequested: Boolean,
        description: String?,
        exitAt: Long,
        lastUpdateAt: Long?,
        separatesUpdates: Boolean,
    ): Boolean {
        if (!userRequested) return false
        if (description?.contains("remove task", ignoreCase = true) == true) return false
        if (!separatesUpdates && lastUpdateAt != null && abs(exitAt - lastUpdateAt) <= UPDATE_MARGIN_MS) return false
        return true
    }
}
