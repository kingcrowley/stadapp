package cc.stadapp.stad

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import cc.stadapp.stad.ui.BlockListViewModel
import cc.stadapp.stad.ui.HomeScreen
import cc.stadapp.stad.ui.theme.StadTheme

class MainActivity : ComponentActivity() {

    private val blockList: BlockListViewModel by viewModels()

    // The user turns the service on and off in system settings, so it's re-read on every onStart
    // and whenever the setting changes while Stad is visible (e.g. split screen).
    private var serviceEnabled by mutableStateOf(false)
    private var settingsUnavailable by mutableStateOf(false)
    private val serviceObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = readServiceEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StadTheme {
                val state by blockList.state.collectAsStateWithLifecycle()
                val lockFailed by blockList.lockFailed.collectAsStateWithLifecycle()
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
                        protectionUnknown = state.protectionUnknown,
                        lockFailed = lockFailed,
                        settingsUnavailable = settingsUnavailable && !serviceEnabled,
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
        readServiceEnabled()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            serviceObserver,
        )
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(serviceObserver)
        super.onStop()
    }

    private fun readServiceEnabled() {
        serviceEnabled = AccessibilityStatus.isEnabled(
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            packageName,
            BlockerAccessibilityService::class.java.name,
        )
    }

    /** Opens Accessibility settings, or general Settings on a phone without that screen. */
    private fun openAccessibilitySettings() {
        settingsUnavailable = false
        for (action in listOf(Settings.ACTION_ACCESSIBILITY_SETTINGS, Settings.ACTION_SETTINGS)) {
            try {
                startActivity(Intent(action))
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next one.
            }
        }
        settingsUnavailable = true
    }
}
