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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BlockListUiState(
    val loading: Boolean = true,
    val rows: List<PickerRow> = emptyList(),
    val blockedCount: Int = 0,
    val editable: Boolean = true,
    val lockState: LockState = LockState.Unlocked,
    val query: String = "",
)

/** Thin glue: gathers facts from Android, asks [BlockListRules], exposes UI state. */
class BlockListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DataStoreBlockListRepository(app.stadSettings)
    private val installedApps = InstalledApps(app)
    private val protectedPackages = ProtectedPackages(app)
    val icons = AppIcons(app)

    private val apps = MutableStateFlow<List<AppEntry>?>(null)
    private val protected = MutableStateFlow(BlockListRules.ALWAYS_PROTECTED)
    private val query = MutableStateFlow("")

    // TODO(slice1.2-lock-state): driven by the debug lock toggle in 1.2, persisted in 2.1.
    private val lockState = MutableStateFlow(LockState.Unlocked)

    val state: StateFlow<BlockListUiState> =
        combine(repository.blockList, apps, protected, lockState, query) { list, installed, prot, lock, q ->
            BlockListUiState(
                loading = installed == null,
                rows = BlockListRules.pickerRows(installed.orEmpty(), list, prot, lock, q),
                blockedCount = BlockListRules.blockedCount(list, prot),
                editable = BlockListRules.canEdit(lock),
                lockState = lock,
                query = q,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockListUiState())

    /** Re-reads installed and protected apps, then drops uninstalled apps from the stored list. */
    fun refresh() {
        viewModelScope.launch {
            val (installed, prot) = withContext(Dispatchers.IO) {
                installedApps.launchableApps() to protectedPackages.current()
            }
            apps.value = installed
            protected.value = prot
            withContext(Dispatchers.IO) {
                repository.update { saved ->
                    val sanitized = BlockListRules.sanitize(saved, installedApps::isInstalled)
                    if (BlockListRules.shouldPersistSanitized(saved, sanitized, lockState.value, installed.size)) {
                        sanitized
                    } else {
                        saved
                    }
                }
            }
        }
    }

    fun setBlocked(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.update {
                BlockListRules.setBlocked(it, packageName, blocked, lockState.value, protected.value)
            }
        }
    }

    fun setQuery(text: String) {
        query.value = text
    }
}
