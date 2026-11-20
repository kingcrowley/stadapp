package cc.stadapp.stad

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.ContentObserver
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import cc.stadapp.stad.core.PairingFlow
import cc.stadapp.stad.core.TagFeedback
import cc.stadapp.stad.core.TagId
import cc.stadapp.stad.data.TagRepository
import cc.stadapp.stad.data.stadSettings
import cc.stadapp.stad.service.Haptics
import cc.stadapp.stad.service.TagTaps
import cc.stadapp.stad.service.TagWriter
import cc.stadapp.stad.service.BlockerAccessibilityService
import cc.stadapp.stad.ui.AppPickerScreen
import cc.stadapp.stad.ui.BlockListViewModel
import cc.stadapp.stad.ui.HomeScreen
import cc.stadapp.stad.ui.NfcStatus
import cc.stadapp.stad.ui.theme.StadTheme
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val blockList: BlockListViewModel by viewModels()

    // The user turns the service on and off in system settings, so it's re-read on every onStart
    // and whenever the setting changes while Stad is visible (e.g. split screen).
    private var serviceEnabled by mutableStateOf(false)
    private var settingsUnavailable by mutableStateOf(false)
    private val serviceObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = readServiceEnabled()
    }

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(this) }
    private var nfcStatus by mutableStateOf(NfcStatus.On)
    private var nfcSettingsUnavailable by mutableStateOf(false)
    private val tags by lazy { TagRepository(stadSettings) }

    // The pairing dialog's session, or null when it's closed. Written from the NFC reader thread
    // with compareAndSet. Each opening gets a new id, so a slow write from a cancelled session
    // can't land in the next one (their states alone can be equal).
    private data class PairingSession(val id: Long, val state: PairingFlow.State)
    private val pairing = MutableStateFlow<PairingSession?>(null)
    private val pairingIds = AtomicLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StadTheme {
                val state by blockList.state.collectAsStateWithLifecycle()
                val lockFailed by blockList.lockFailed.collectAsStateWithLifecycle()
                val tagPaired by blockList.tagPaired.collectAsStateWithLifecycle()
                val pairingSession by pairing.collectAsStateWithLifecycle()
                // Two screens and a dialog, so plain state. TODO(slice2.x-navigation):
                // navigation-compose if more screens arrive.
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
                        tagPaired = tagPaired,
                        nfcStatus = nfcStatus,
                        nfcSettingsUnavailable = nfcSettingsUnavailable && nfcStatus == NfcStatus.Off,
                        pairing = pairingSession?.state,
                        showDebugLock = BuildConfig.DEBUG,
                        onChooseApps = { showPicker = true },
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onOpenNfcSettings = ::openNfcSettings,
                        onPairTag = {
                            pairing.value = PairingSession(pairingIds.incrementAndGet(), PairingFlow.State())
                        },
                        onClosePairing = { pairing.value = null },
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

    override fun onResume() {
        super.onResume()
        readNfcStatus()
        // While Stad is open it reads tags itself (for pairing, and so a tag works here even if
        // Android's dispatch would go elsewhere). This also stops Android starting TagActivity
        // for the same tap. A, B, F and V so bank and transit cards are seen too.
        nfcAdapter?.enableReaderMode(this, ::onTag, READER_FLAGS, null)
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
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

    private fun readNfcStatus() {
        val adapter = nfcAdapter
        nfcStatus = when {
            adapter == null -> NfcStatus.Missing
            !adapter.isEnabled -> NfcStatus.Off
            else -> NfcStatus.On
        }
    }

    /** Called on the NFC reader thread, so blocking tag and storage I/O is fine here. */
    private fun onTag(tag: Tag) {
        val current = pairing.value
        if (current != null) {
            onPairingTap(current, tag)
            return
        }
        val id = TagId.fromBytes(tag.id) ?: return
        if (!TagTaps.debounce.accept(id, SystemClock.elapsedRealtime())) return
        val feedback = runBlocking { TagTaps.handle(this@MainActivity, id) }
        runOnUiThread { TagTaps.show(this, feedback) }
    }

    private fun onPairingTap(current: PairingSession, tag: Tag) {
        var next = current.copy(
            state = PairingFlow.onTap(
                current.state, tag.id, { TagWriter.write(tag) }, { TagWriter.verifyOrRewrite(tag) },
            ),
        )
        val saving = next.state.step as? PairingFlow.Step.Saving
        if (saving != null) {
            if (!pairing.compareAndSet(current, next)) return // cancelled meanwhile
            val saved = try {
                runBlocking { tags.pair(saving.id) }
            } catch (_: IOException) {
                null
            }
            val after = next.copy(state = PairingFlow.afterSave(next.state, saved))
            if (!pairing.compareAndSet(next, after)) return
            next = after
        } else if (!pairing.compareAndSet(current, next)) {
            return
        }
        val haptic = when {
            next.state.step is PairingFlow.Step.Done -> TagFeedback.Haptic.One
            next.state.error != null -> TagFeedback.Haptic.Three
            else -> TagFeedback.Haptic.None
        }
        runOnUiThread { Haptics.play(this, haptic) }
    }

    /** Opens NFC settings, falling back to wireless settings, then Settings. */
    private fun openNfcSettings() {
        nfcSettingsUnavailable = !startFirst(
            Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS,
        )
    }

    private fun startFirst(vararg actions: String): Boolean {
        for (action in actions) {
            try {
                startActivity(Intent(action))
                return true
            } catch (_: ActivityNotFoundException) {
                // Try the next one.
            }
        }
        return false
    }

    /** Opens Accessibility settings, or general Settings on a phone without that screen. */
    private fun openAccessibilitySettings() {
        settingsUnavailable = !startFirst(Settings.ACTION_ACCESSIBILITY_SETTINGS, Settings.ACTION_SETTINGS)
    }

    private companion object {
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V
    }
}
