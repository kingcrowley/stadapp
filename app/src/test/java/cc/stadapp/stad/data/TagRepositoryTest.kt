package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.TagOutcome
import java.io.File
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TagRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "stad_settings.preferences_pb") }

    @Test
    fun noTagPairedByDefault() = runTest {
        assertNull(TagRepository(store()).pairedTag.first())
    }

    @Test
    fun pairStoresNormalizedId() = runTest {
        val repo = TagRepository(store())
        assertTrue(repo.pair("04:a1:b2"))
        assertEquals("04A1B2", repo.pairedTag.first())
    }

    @Test
    fun pairIsRefusedWhileLocked() = runTest {
        val store = store()
        val repo = TagRepository(store)
        repo.pair("04A1")
        LockStateRepository(store).set(LockState.Locked)
        assertFalse(repo.pair("0499"))
        assertEquals("04A1", repo.pairedTag.first())
    }

    @Test
    fun pairedTagTogglesTheLock() = runTest {
        val store = store()
        val repo = TagRepository(store)
        val lock = LockStateRepository(store)
        repo.pair("04A1")
        assertEquals(TagOutcome.Lock, repo.tap("04A1"))
        assertEquals(LockState.Locked, lock.lockState.first())
        assertEquals(TagOutcome.Unlock, repo.tap("04a1"))
        assertEquals(LockState.Unlocked, lock.lockState.first())
    }

    @Test
    fun otherTagLeavesTheLockAlone() = runTest {
        val store = store()
        val repo = TagRepository(store)
        val lock = LockStateRepository(store)
        repo.pair("04A1")
        repo.tap("04A1")
        assertEquals(TagOutcome.NotYourTag, repo.tap("0499"))
        assertEquals(LockState.Locked, lock.lockState.first())
    }

    @Test
    fun tapWithNoPairedTagNeverLocks() = runTest {
        val store = store()
        assertEquals(TagOutcome.NotPaired, TagRepository(store).tap("04A1"))
        assertEquals(LockState.Unlocked, LockStateRepository(store).lockState.first())
    }

    @Test
    fun concurrentTapsAndPairsSerialise() = runTest {
        val store = store()
        val repo = TagRepository(store)
        repo.pair("04A1")
        // Each tap reads the state the previous one wrote: an even number of taps ends unlocked.
        (1..10).map { async { repo.tap("04A1") } }.awaitAll()
        assertEquals(LockState.Unlocked, LockStateRepository(store).lockState.first())
        // A pair racing a locking tap: either the pair lands first and the tap is NotYourTag,
        // or the tap locks first and the pair is refused. Never both a lock and a new tag.
        val results = listOf(async { repo.tap("04A1") }, async { repo.pair("0499") }).awaitAll()
        val locked = LockStateRepository(store).lockState.first().isLocked
        val tag = repo.pairedTag.first()
        if (locked) {
            assertEquals("04A1", tag)
            assertEquals(false, results[1])
        } else {
            assertEquals("0499", tag)
            assertEquals(TagOutcome.NotYourTag, results[0])
        }
    }

    @Test
    fun failedWriteLeavesTheLockUnchanged() = runTest {
        val real = store()
        TagRepository(real).pair("04A1")
        val failing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = real.data
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }
        try {
            TagRepository(failing).tap("04A1")
            fail("Expected IOException")
        } catch (_: IOException) {
            // Expected.
        }
        assertEquals(LockState.Unlocked, LockStateRepository(real).lockState.first())
    }
}
