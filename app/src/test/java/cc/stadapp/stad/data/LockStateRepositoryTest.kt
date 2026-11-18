package cc.stadapp.stad.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import cc.stadapp.stad.core.LockState
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LockStateRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(): File = File(tmp.root, "stad_settings.preferences_pb")

    @Test
    fun unlockedByDefault() = runTest {
        val repo = LockStateRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file))
        assertEquals(LockState.Unlocked, repo.lockState.first())
    }

    @Test
    fun setIsReadBack() = runTest {
        val repo = LockStateRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file))
        repo.set(LockState.Locked)
        assertEquals(LockState.Locked, repo.lockState.first())
        repo.set(LockState.Unlocked)
        assertEquals(LockState.Unlocked, repo.lockState.first())
    }

    @Test
    fun unknownStoredValueReadsAsUnlocked() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file)
        store.edit { it[StadKeys.LOCK_STATE] = "Bogus" }
        assertEquals(LockState.Unlocked, LockStateRepository(store).lockState.first())
    }
}
