package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import cc.stadapp.stad.core.EscapeLog
import cc.stadapp.stad.core.EscapeState
import cc.stadapp.stad.core.EscapeTracker
import cc.stadapp.stad.core.EscapeUpdate
import cc.stadapp.stad.core.LockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The blocking history (when blocking was off while locked) and the state behind it, on the
 * shared DataStore. Each transition reads the lock and writes in one edit, so the lock and the
 * history can't drift apart. On the phone only; never logged or sent.
 */
class EscapeRepository(private val dataStore: DataStore<Preferences>) {

    /** The history. A read error or corrupt value reads as empty. */
    val log: Flow<EscapeLog> = dataStore.data
        .retryingOnReadError()
        .map { EscapeLog.decode(it[StadKeys.ESCAPE_LOG]) }
        .distinctUntilChanged()

    /** The service is stopping. [settingOn]: whether the enabled-services setting still lists Stad. */
    suspend fun onServiceStopped(settingOn: Boolean, now: Long) = transition { state, locked ->
        EscapeTracker.onServiceStopped(state, locked, settingOn, now)
    }

    suspend fun onServiceConnected(forceStoppedAt: Long?, now: Long) = transition { state, locked ->
        EscapeTracker.onServiceConnected(state, locked, forceStoppedAt, now)
    }

    /** Stad is running and found the service off. */
    suspend fun onFoundOff(forceStoppedAt: Long?, now: Long) = transition { state, locked ->
        EscapeTracker.onFoundOff(state, locked, forceStoppedAt, now)
    }

    /** Refreshes "last seen working" if it's due. Cheap when it isn't (a read, no write). */
    suspend fun touchAlive(now: Long) {
        dataStore.edit { prefs ->
            val state = prefs.escapeState()
            if (EscapeTracker.shouldTouchAlive(state, prefs.isLocked(), now)) prefs[StadKeys.ESCAPE_LAST_ALIVE] = now
        }
    }

    /** Clears the history. Refused (returns false) while locked: nothing about blocking changes then. */
    suspend fun clear(): Boolean {
        var cleared = false
        dataStore.edit { prefs ->
            if (prefs.isLocked()) return@edit
            prefs.remove(StadKeys.ESCAPE_LOG)
            cleared = true
        }
        return cleared
    }

    private suspend fun transition(change: (EscapeState, Boolean) -> EscapeUpdate) {
        dataStore.edit { prefs -> prefs.applyEscape(change(prefs.escapeState(), prefs.isLocked())) }
    }
}

private fun Preferences.isLocked() = LockState.fromStored(this[StadKeys.LOCK_STATE]).isLocked

internal fun Preferences.escapeState() = EscapeState(
    offSince = this[StadKeys.ESCAPE_OFF_SINCE],
    offSinceApprox = this[StadKeys.ESCAPE_OFF_APPROX] == true,
    lastAlive = this[StadKeys.ESCAPE_LAST_ALIVE],
    lockedAt = this[StadKeys.ESCAPE_LOCKED_AT],
    serviceConnected = this[StadKeys.ESCAPE_CONNECTED] == true,
    connectedAt = this[StadKeys.ESCAPE_CONNECTED_AT],
)

/** Writes [update]'s state and appends its entry, if any. */
internal fun MutablePreferences.applyEscape(update: EscapeUpdate) {
    val s = update.state
    setOrRemove(StadKeys.ESCAPE_OFF_SINCE, s.offSince)
    this[StadKeys.ESCAPE_OFF_APPROX] = s.offSinceApprox
    setOrRemove(StadKeys.ESCAPE_LAST_ALIVE, s.lastAlive)
    setOrRemove(StadKeys.ESCAPE_LOCKED_AT, s.lockedAt)
    this[StadKeys.ESCAPE_CONNECTED] = s.serviceConnected
    setOrRemove(StadKeys.ESCAPE_CONNECTED_AT, s.connectedAt)
    update.entry?.let { this[StadKeys.ESCAPE_LOG] = EscapeLog.decode(this[StadKeys.ESCAPE_LOG]).add(it).encode() }
}

private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
    if (value == null) remove(key) else this[key] = value
}
