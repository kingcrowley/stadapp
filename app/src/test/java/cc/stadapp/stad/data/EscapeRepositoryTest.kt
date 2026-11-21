package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import cc.stadapp.stad.core.EscapeEntry
import cc.stadapp.stad.core.EscapeEntry.EndedBy
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.TagOutcome
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EscapeRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val min = 60_000L

    private fun TestScope.store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "stad_settings.preferences_pb") }

    @Test
    fun offWhileLockedIsRecordedAndSurvivesReopening() = runTest {
        val store = store()
        LockStateRepository(store).set(LockState.Locked, now = 0)
        val repo = EscapeRepository(store)
        repo.onServiceConnected(forceStoppedAt = null, now = 1 * min)
        repo.onServiceStopped(settingOn = false, now = 10 * min)
        repo.onServiceConnected(forceStoppedAt = null, now = 25 * min)
        assertEquals(
            listOf(EscapeEntry(10 * min, 25 * min, false, EndedBy.ServiceBack)),
            EscapeRepository(store).log.first().entries,
        )
    }

    @Test
    fun tagUnlockEndsAPeriodInTheSameEdit() = runTest {
        val store = store()
        val tags = TagRepository(store)
        tags.pair("04A1")
        assertEquals(TagOutcome.Lock, tags.tap("04A1", serviceOn = true, now = 0))
        // Force-stopped at 5 min, unlocked by tag at 30 min without Stad opening in between.
        assertEquals(TagOutcome.Unlock, tags.tap("04A1", serviceOn = false, forceStoppedAt = 5 * min, now = 30 * min))
        assertEquals(
            listOf(EscapeEntry(5 * min, 30 * min, false, EndedBy.Unlock)),
            EscapeRepository(store).log.first().entries,
        )
    }

    @Test
    fun clearIsRefusedWhileLocked() = runTest {
        val store = store()
        val lock = LockStateRepository(store)
        val repo = EscapeRepository(store)
        lock.set(LockState.Locked, now = 0)
        repo.onServiceStopped(settingOn = false, now = 1 * min)
        lock.set(LockState.Unlocked, serviceOn = false, now = 10 * min)
        lock.set(LockState.Locked, now = 11 * min)
        assertFalse(repo.clear())
        assertEquals(1, repo.log.first().entries.size)
        lock.set(LockState.Unlocked, now = 12 * min)
        assertTrue(repo.clear())
        assertTrue(repo.log.first().entries.isEmpty())
    }
}
