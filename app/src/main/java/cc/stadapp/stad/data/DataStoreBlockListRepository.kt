package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import cc.stadapp.stad.core.BlockList
import cc.stadapp.stad.core.LockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreBlockListRepository(
    private val dataStore: DataStore<Preferences>,
) : BlockListRepository {

    override val blockList: Flow<BlockList> = dataStore.data
        .retryingOnReadError()
        .map { prefs -> BlockList(prefs[BLOCKED_PACKAGES] ?: emptySet()) }

    override suspend fun update(transform: (BlockList) -> BlockList) {
        dataStore.edit { prefs ->
            // Read the lock in the same edit, so a change can't land just after locking.
            if (LockState.fromStored(prefs[StadKeys.LOCK_STATE]).isLocked) return@edit
            val current = BlockList(prefs[BLOCKED_PACKAGES] ?: emptySet())
            val next = transform(current)
            if (next != current) prefs[BLOCKED_PACKAGES] = next.packages
        }
    }

    companion object {
        val BLOCKED_PACKAGES = StadKeys.BLOCKED_PACKAGES
    }
}
