package cc.stadapp.stad.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.stadapp.stad.core.AppEntry
import cc.stadapp.stad.core.BlockListRules
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.PickerRow
import cc.stadapp.stad.data.DataStoreBlockListRepository
import cc.stadapp.stad.data.stadSettings
import cc.stadapp.stad.service.AppIcons
import cc.stadapp.stad.service.InstalledApps
import cc.stadapp.stad.service.ProtectedPackages
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Why the picker is read-only or showing a warning, if it is. */
enum class BlockListProblem { None, ProtectionUnknown, SaveFailed }

data class BlockListUiState(
    val loading: Boolean = true,
    val rows: List<PickerRow> = emptyList(),
    val blockedCount: Int = 0,
    val editable: Boolean = false,
    val lockState: LockState = LockState.Unlocked,
    val query: String = "",
    val problem: BlockListProblem = BlockListProblem.None,
)

/** What the device told us on the last refresh. Published as one value so it can't be mixed. */
private data class Discovery(
    val apps: List<AppEntry>,
    val protectedPackages: Set<String>,
    val protectionComplete: Boolean,
)

/** Thin glue: gathers facts from Android, asks [BlockListRules], exposes UI state. */
class BlockListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DataStoreBlockListRepository(app.stadSettings)
    private val installedApps = InstalledApps(app)
    private val protectedPackages = ProtectedPackages(app)
    val icons = AppIcons(app)

    // Null until the first refresh: nothing is editable before protection is known.
    private val discovery = MutableStateFlow<Discovery?>(null)
    private val query = MutableStateFlow("")
    // Each kind of write keeps its own failure, so a later successful toggle can't hide a failed clean-up.
    private val editFailed = MutableStateFlow(false)
    private val cleanUpFailed = MutableStateFlow(false)
    private val saveFailed = combine(editFailed, cleanUpFailed) { edit, cleanUp -> edit || cleanUp }
    private var refreshJob: Job? = null

    // TODO(slice1.2-lock-state): driven by the debug lock toggle in 1.2, persisted in 2.1.
    private val lockState = MutableStateFlow(LockState.Unlocked)

    val state: StateFlow<BlockListUiState> =
        combine(repository.blockList, discovery, lockState, query, saveFailed) { list, found, lock, q, failed ->
            val prot = found?.protectedPackages ?: BlockListRules.ALWAYS_PROTECTED
            val complete = found?.protectionComplete ?: false
            BlockListUiState(
                loading = found == null,
                rows = BlockListRules.pickerRows(found?.apps.orEmpty(), list, prot, lock, q, complete),
                blockedCount = BlockListRules.blockedCount(list, prot),
                editable = BlockListRules.canEdit(lock, complete),
                lockState = lock,
                query = q,
                problem = when {
                    failed -> BlockListProblem.SaveFailed
                    found != null && !complete -> BlockListProblem.ProtectionUnknown
                    else -> BlockListProblem.None
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockListUiState())

    /**
     * Re-reads installed and protected apps, then drops uninstalled apps from the stored list.
     * A newer refresh cancels an older one, so stale results are never published.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                val result = protectedPackages.current()
                Discovery(installedApps.launchableApps(), result.packages, result.complete)
            }
            discovery.value = found
            cleanUp(found)
        }
    }

    /** Drops uninstalled apps from the stored list. A failure stays flagged until a clean-up succeeds. */
    private suspend fun cleanUp(found: Discovery) {
        cleanUpFailed.value = !save {
            repository.update { saved ->
                val sanitized = BlockListRules.sanitize(saved, installedApps::isInstalled)
                if (BlockListRules.shouldPersistSanitized(saved, sanitized, lockState.value, found.apps.size)) {
                    sanitized
                } else {
                    saved
                }
            }
        }
    }

    fun setBlocked(packageName: String, blocked: Boolean) {
        val found = discovery.value ?: return
        viewModelScope.launch {
            editFailed.value = !save {
                repository.update {
                    BlockListRules.setBlocked(
                        it, packageName, blocked, lockState.value,
                        found.protectedPackages, found.protectionComplete,
                    )
                }
            }
            // Storage works again, so retry a clean-up that failed earlier.
            if (!editFailed.value && cleanUpFailed.value) cleanUp(found)
        }
    }

    fun setQuery(text: String) {
        query.value = text
    }

    /** Runs a write and returns whether it succeeded. On failure the stored list is unchanged. */
    private suspend fun save(write: suspend () -> Unit): Boolean =
        try {
            withContext(Dispatchers.IO) { write() }
            true
        } catch (_: IOException) {
            false
        }
}
