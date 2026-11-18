package cc.stadapp.stad.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cc.stadapp.stad.core.BlockList
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.map

/** The single app-wide settings store. One instance per file, as DataStore requires. */
val Context.stadSettings: DataStore<Preferences> by preferencesDataStore(
    name = "stad_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class DataStoreBlockListRepository(
    private val dataStore: DataStore<Preferences>,
) : BlockListRepository {

    /**
     * On a read error, shows an empty list and keeps retrying with back-off (1 s, 2 s, ... 30 s),
     * so observation recovers once storage does. Other errors propagate.
     */
    override val blockList: Flow<BlockList> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(minOf(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MS))
            true
        }
        .map { prefs -> BlockList(prefs[BLOCKED_PACKAGES] ?: emptySet()) }

    override suspend fun update(transform: (BlockList) -> BlockList) {
        dataStore.edit { prefs ->
            val current = BlockList(prefs[BLOCKED_PACKAGES] ?: emptySet())
            val next = transform(current)
            if (next != current) prefs[BLOCKED_PACKAGES] = next.packages
        }
    }

    companion object {
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        private const val MAX_RETRY_DELAY_MS = 30_000L
    }
}
