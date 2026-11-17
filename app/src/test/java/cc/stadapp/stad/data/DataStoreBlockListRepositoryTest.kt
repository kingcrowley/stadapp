package cc.stadapp.stad.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cc.stadapp.stad.core.BlockList
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
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
