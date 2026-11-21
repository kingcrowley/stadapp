package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import cc.stadapp.stad.core.EscapeTracker
import cc.stadapp.stad.core.LockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The lock state, shared by the app and the blocking service. Missing, unknown or unreadable
 * values read as [LockState.Unlocked]: failing open is deliberate, so a storage fault can never
 * trap the user.
 */
class LockStateRepository(private val dataStore: DataStore<Preferences>) {

    val lockState: Flow<LockState> = dataStore.data
        .retryingOnReadError()
        .map { prefs -> LockState.fromStored(prefs[StadKeys.LOCK_STATE]) }
        .distinctUntilChanged()

    /**
     * Sets the lock (debug builds and tests; the tag goes through [TagRepository.tap]), updating
     * the blocking history in the same edit. Throws [java.io.IOException] if the write fails.
     */
    suspend fun set(
        state: LockState,
        blocking: BlockingProbe = BlockingProbe.Fixed(),
        now: Long = System.currentTimeMillis(),
    ) {
        dataStore.edit { prefs ->
            val was = LockState.fromStored(prefs[StadKeys.LOCK_STATE])
            prefs[StadKeys.LOCK_STATE] = state.name
            if (was == state) return@edit
            val escape = prefs.escapeState()
            val on = blocking.serviceOn()
            prefs.applyEscape(
                if (state.isLocked) EscapeTracker.onLocked(escape, on, now)
                else EscapeTracker.onUnlocked(escape, on, if (on) null else blocking.forceStoppedAt(), now),
            )
        }
    }
}
