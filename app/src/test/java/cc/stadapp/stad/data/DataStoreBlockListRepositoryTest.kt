package cc.stadapp.stad.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cc.stadapp.stad.core.BlockList
import cc.stadapp.stad.core.LockState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreBlockListRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(): File = File(tmp.root, "stad_settings.preferences_pb")

    private fun TestScope.repo(scope: CoroutineScope = backgroundScope) =
        DataStoreBlockListRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = ::file)
        )

    @Test
    fun emptyByDefault() = runTest {
        assertTrue(repo().blockList.first().isEmpty())
    }

    @Test
    fun updateAddsAndRemoves() = runTest {
        val repo = repo()
        repo.update { it.with("a", true).with("b", true) }
        assertEquals(setOf("a", "b"), repo.blockList.first().packages)
        repo.update { it.with("a", false) }
        assertEquals(setOf("b"), repo.blockList.first().packages)
    }

    @Test
    fun updatesAreAppliedInOrderWithoutLosingAny() = runTest {
        val repo = repo()
        (1..20).forEach { n -> repo.update { it.with("p$n", true) } }
        assertEquals(20, repo.blockList.first().size)
    }

    @Test
    fun overlappingUpdatesAreNotLost() = runTest {
        val repo = repo()
        val jobs = (1..20).map { n -> launch { repo.update { it.with("p$n", true) } } }
        jobs.joinAll()
        assertEquals((1..20).map { "p$it" }.toSet(), repo.blockList.first().packages)
    }

    @Test
    fun editWhileLockedLeavesListUnchanged() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file)
        val repo = DataStoreBlockListRepository(store)
        repo.update { it.with("kept", true) }
        LockStateRepository(store).set(LockState.Locked)
        repo.update { it.with("kept", false).with("added", true) }
        assertEquals(setOf("kept"), repo.blockList.first().packages)
    }

    @Test
    fun editAfterUnlockingApplies() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file)
        val repo = DataStoreBlockListRepository(store)
        val lock = LockStateRepository(store)
        lock.set(LockState.Locked)
        lock.set(LockState.Unlocked)
        repo.update { it.with("added", true) }
        assertEquals(setOf("added"), repo.blockList.first().packages)
    }

    @Test
    fun readErrorShowsEmptyThenRecovers() = runTest {
        val store = FlakyReadStore(failuresBeforeSuccess = 1, initial = setOf("kept"))
        val seen = DataStoreBlockListRepository(store).blockList.take(2).toList()
        assertEquals(listOf(emptySet(), setOf("kept")), seen.map { it.packages })
    }

    @Test
    fun survivesANewInstanceOnTheSameFile() = runTest {
        val firstJob = Job()
        val firstScope = CoroutineScope(StandardTestDispatcher(testScheduler) + firstJob)
        repo(firstScope).update { BlockList(setOf("kept")) }
        firstJob.cancelAndJoin()

        assertEquals(setOf("kept"), repo().blockList.first().packages)
    }

    @Test
    fun corruptFileReadsAsEmpty() = runTest {
        file().writeBytes(byteArrayOf(0x7f, 0x00, 0x13, 0x42))
        val repo = DataStoreBlockListRepository(
            PreferenceDataStoreFactory.create(
                corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
                    androidx.datastore.preferences.core.emptyPreferences()
                },
                scope = backgroundScope,
                produceFile = ::file,
            )
        )
        assertTrue(repo.blockList.first().isEmpty())
    }
}

/** A DataStore whose reads throw IOException a set number of times before succeeding. */
private class FlakyReadStore(
    private var failuresBeforeSuccess: Int,
    initial: Set<String>,
) : DataStore<Preferences> {
    private val state = MutableStateFlow(
        mutablePreferencesOf(DataStoreBlockListRepository.BLOCKED_PACKAGES to initial) as Preferences
    )

    override val data: Flow<Preferences> = flow {
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            throw IOException("simulated read failure")
        }
        emitAll(state)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
