package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    /** Throws [java.io.IOException] if the write fails. */
    suspend fun set(state: LockState) {
        dataStore.edit { it[StadKeys.LOCK_STATE] = state.name }
    }
}
