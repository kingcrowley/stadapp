package cc.stadapp.stad

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.stadapp.stad.core.AccessibilityStatus
import cc.stadapp.stad.service.BlockerAccessibilityService
import cc.stadapp.stad.ui.AppPickerScreen
import cc.stadapp.stad.ui.BlockListProblem
import cc.stadapp.stad.ui.BlockListViewModel
import cc.stadapp.stad.ui.HomeScreen
import cc.stadapp.stad.ui.theme.StadTheme

class MainActivity : ComponentActivity() {

    private val blockList: BlockListViewModel by viewModels()

    // Re-read on every onStart, since the user turns the service on in system settings.
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StadTheme {
                val state by blockList.state.collectAsStateWithLifecycle()
                // Two screens, so plain state. TODO(slice1.3-navigation): navigation-compose
                // when the pair-tag screen arrives.
                var showPicker by rememberSaveable { mutableStateOf(false) }
                if (showPicker) {
                    BackHandler {
                        blockList.setQuery("")
                        showPicker = false
                    }
                    AppPickerScreen(
                        state = state,
                        loadIcon = blockList.icons::load,
                        onQueryChange = blockList::setQuery,
                        onToggle = blockList::setBlocked,
                    )
                } else {
                    HomeScreen(
                        state = state.lockState,
                        blockedCount = state.blockedCount,
                        serviceEnabled = serviceEnabled,
                        protectionUnknown = state.problem == BlockListProblem.ProtectionUnknown,
                        showDebugLock = BuildConfig.DEBUG,
                        onChooseApps = { showPicker = true },
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onDebugLock = blockList::setLockedForDebug,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Picks up installs, uninstalls and launcher changes made while Stad was in the background.
        blockList.refresh()
        serviceEnabled = AccessibilityStatus.isEnabled(
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            packageName,
            BlockerAccessibilityService::class.java.name,
        )
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
