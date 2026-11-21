package cc.stadapp.stad.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NotifyPromptRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(): File = File(tmp.root, "stad_settings.preferences_pb")

    @Test
    fun notDismissedByDefault() = runTest {
        val repo = NotifyPromptRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file))
        assertFalse(repo.dismissed.first())
    }

    @Test
    fun dismissIsRemembered() = runTest {
        val repo = NotifyPromptRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = ::file))
        repo.dismiss()
        assertTrue(repo.dismissed.first())
    }
}
