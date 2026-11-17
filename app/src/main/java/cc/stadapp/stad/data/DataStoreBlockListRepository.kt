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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The single app-wide settings store. One instance per file, as DataStore requires. */
val Context.stadSettings: DataStore<Preferences> by preferencesDataStore(
    name = "stad_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class DataStoreBlockListRepository(
    private val dataStore: DataStore<Preferences>,
) : BlockListRepository {

    override val blockList: Flow<BlockList> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
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
    }
}
