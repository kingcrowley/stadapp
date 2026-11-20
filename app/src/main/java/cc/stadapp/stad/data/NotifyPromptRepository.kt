package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Whether the user said "Not now" to Home's notification-permission card. A read error reads as not dismissed. */
class NotifyPromptRepository(private val dataStore: DataStore<Preferences>) {

    val dismissed: Flow<Boolean> = dataStore.data
        .retryingOnReadError()
        .map { prefs -> prefs[StadKeys.NOTIFY_PROMPT_DISMISSED] == true }
        .distinctUntilChanged()

    /** Throws [java.io.IOException] if the write fails. */
    suspend fun dismiss() {
        dataStore.edit { it[StadKeys.NOTIFY_PROMPT_DISMISSED] = true }
    }
}
