package cc.stadapp.stad.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

/** The single app-wide settings store, shared by the app and the blocking service. One instance per file, as DataStore requires. */
val Context.stadSettings: DataStore<Preferences> by preferencesDataStore(
    name = "stad_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Keys in [stadSettings]. */
object StadKeys {
    val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
    val LOCK_STATE = stringPreferencesKey("lock_state")
    val PAIRED_TAG = stringPreferencesKey("paired_tag")
    val NOTIFY_PROMPT_DISMISSED = booleanPreferencesKey("notify_prompt_dismissed")

    // Blocking history (slice 2.2); see EscapeRepository.
    val ESCAPE_LOG = stringPreferencesKey("escape_log")
    val ESCAPE_OFF_SINCE = longPreferencesKey("escape_off_since")
    val ESCAPE_OFF_APPROX = booleanPreferencesKey("escape_off_approx")
    val ESCAPE_LAST_ALIVE = longPreferencesKey("escape_last_alive")
    val ESCAPE_LOCKED_AT = longPreferencesKey("escape_locked_at")
    val ESCAPE_CONNECTED = booleanPreferencesKey("escape_connected")
    val ESCAPE_CONNECTED_AT = longPreferencesKey("escape_connected_at")
}

private const val MAX_RETRY_DELAY_MS = 30_000L

/**
 * On a read error, emits empty preferences and keeps retrying with back-off (1 s, 2 s, ... 30 s),
 * so observation recovers once storage does. Other errors propagate.
 */
internal fun Flow<Preferences>.retryingOnReadError(): Flow<Preferences> =
    retryWhen { cause, attempt ->
        if (cause !is IOException) return@retryWhen false
        emit(emptyPreferences())
        delay(minOf(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MS))
        true
    }
