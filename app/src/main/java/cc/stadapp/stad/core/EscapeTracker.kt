package cc.stadapp.stad.core

/**
 * What Stad remembers between moments it runs, to tell when blocking was off while locked.
 * Wall-clock millis throughout.
 */
data class EscapeState(
    /** Blocking has been off since then, while locked. */
    val offSince: Long? = null,
    val offSinceApprox: Boolean = false,
    /** The last time the service was seen working while locked (written sparingly). */
    val lastAlive: Long? = null,
    /** When Stad was last locked; caps any estimated start. */
    val lockedAt: Long? = null,
    /** The service connected and hasn't been cleanly unbound since. */
    val serviceConnected: Boolean = false,
    val connectedAt: Long? = null,
)

/** A new state, plus an entry for the history if an off period just ended. */
data class EscapeUpdate(val state: EscapeState, val entry: EscapeEntry? = null)

/**
 * Pure transitions for the blocking history. Callers apply each one inside a single storage edit,
 * reading the lock in the same edit. Nothing here prevents anything: it only records.
 *
 * `forceStoppedAt` is when Android last recorded a force-stop of Stad, if known (Android 11+ keeps
 * process exit reasons; see [ForceStopRecord]); null otherwise.
 */
object EscapeTracker {

    /** Shorter exact periods are a quick off/on toggle and aren't recorded. */
    const val MIN_EXACT_MS = 5_000L
    /** How often the service refreshes [EscapeState.lastAlive] while locked. */
    const val ALIVE_INTERVAL_MS = 15 * 60 * 1000L

    /**
     * The service is stopping. It counts only if the enabled-services setting no longer lists Stad
     * ([settingOn] false): the user turned it off. Shutdown, a user switch and other system
     * unbinds leave the setting on. Safe to call twice.
     */
    fun onServiceStopped(state: EscapeState, locked: Boolean, settingOn: Boolean, now: Long): EscapeUpdate {
        val s = state.copy(serviceConnected = false)
        if (!locked || settingOn) return EscapeUpdate(s)
        if (s.offSince != null && !s.offSinceApprox) return EscapeUpdate(s) // already recorded
        // An exact time beats an estimate a found-off check may have written first.
        return EscapeUpdate(s.copy(offSince = now, offSinceApprox = false))
    }

    /** Stad is running, locked, and the service is off, e.g. after a force-stop when nothing ran. */
    fun onFoundOff(state: EscapeState, locked: Boolean, forceStoppedAt: Long?, now: Long): EscapeUpdate {
        if (!locked || state.offSince != null) return EscapeUpdate(state)
        val (start, approx) = estimatedStart(state, forceStoppedAt, now)
        return EscapeUpdate(state.copy(offSince = start, offSinceApprox = approx))
    }

    /** The service connected. Closes an open off period, or spots a force-stop nothing saw. */
    fun onServiceConnected(state: EscapeState, locked: Boolean, forceStoppedAt: Long?, now: Long): EscapeUpdate {
        val fresh = state.copy(
            offSince = null, offSinceApprox = false, serviceConnected = true, connectedAt = now,
            lastAlive = if (locked) now else state.lastAlive,
        )
        if (!locked) return EscapeUpdate(fresh)
        val entry = when {
            state.offSince != null ->
                close(state.offSince, state.offSinceApprox, now, EscapeEntry.EndedBy.ServiceBack)
            // Killed and turned back on straight from Settings, without Stad running in between:
            // the last connection never unbound, and Android says the user stopped Stad since.
            state.serviceConnected && forceStoppedAt != null && forceStoppedAt > (state.connectedAt ?: Long.MIN_VALUE) -> {
                val (start, approx) = estimatedStart(state, forceStoppedAt, now)
                close(start, approx, now, EscapeEntry.EndedBy.ServiceBack)
            }
            else -> null
        }
        return EscapeUpdate(fresh, entry)
    }

    /** Locked by the tag. If blocking is already off, the off period starts now. */
    fun onLocked(state: EscapeState, serviceOn: Boolean, now: Long): EscapeUpdate = EscapeUpdate(
        state.copy(
            lockedAt = now,
            offSince = if (serviceOn) null else now,
            offSinceApprox = false,
        ),
    )

    /** Unlocked by the tag. Ends any off period, including one only found now. */
    fun onUnlocked(state: EscapeState, serviceOn: Boolean, forceStoppedAt: Long?, now: Long): EscapeUpdate {
        val open = if (!serviceOn) onFoundOff(state, locked = true, forceStoppedAt, now).state else state
        val entry = open.offSince?.let { close(it, open.offSinceApprox, now, EscapeEntry.EndedBy.Unlock) }
        return EscapeUpdate(open.copy(offSince = null, offSinceApprox = false, lockedAt = null), entry)
    }

    /** Whether the service should refresh [EscapeState.lastAlive] now. */
    fun shouldTouchAlive(state: EscapeState, locked: Boolean, now: Long): Boolean =
        locked && (state.lastAlive == null || now - state.lastAlive >= ALIVE_INTERVAL_MS)

    /**
     * When blocking most likely stopped: Android's force-stop time if it belongs to this off period
     * (after the lock and after the service last connected: an earlier one was already handled),
     * exact; else the last time the service was seen working, an estimate. Never before the lock,
     * and never in the future.
     */
    private fun estimatedStart(state: EscapeState, forceStoppedAt: Long?, now: Long): Pair<Long, Boolean> {
        val floor = state.lockedAt ?: Long.MIN_VALUE
        val lastConnect = state.connectedAt ?: Long.MIN_VALUE
        if (forceStoppedAt != null && forceStoppedAt > lastConnect && forceStoppedAt >= floor && forceStoppedAt <= now) {
            return forceStoppedAt to false
        }
        val guess = maxOf(state.lastAlive ?: now, floor).coerceAtMost(now)
        return guess to true
    }

    private fun close(start: Long, approx: Boolean, end: Long, endedBy: EscapeEntry.EndedBy): EscapeEntry? {
        val entry = EscapeEntry(start, end, approx, endedBy)
        return if (!approx && entry.durationMs < MIN_EXACT_MS) null else entry
    }
}
