package cc.stadapp.stad.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import cc.stadapp.stad.core.BlockDecision
import cc.stadapp.stad.core.BlockList
import cc.stadapp.stad.core.Decision
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.RelaunchGuard
import cc.stadapp.stad.data.DataStoreBlockListRepository
import cc.stadapp.stad.data.LockStateRepository
import cc.stadapp.stad.data.stadSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Covers a blocked app with Stad's block screen while Stad is locked.
 *
 * Play policy (docs/06_PLAY_POLICY.md): subscribes to window-state changes only and never reads
 * window content. It looks at the foreground package and activity class, keeps the last package
 * in memory only, and never stores, logs or sends it.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val guard = RelaunchGuard()
    private val protectedPackages by lazy { ProtectedPackages(this) }

    // All touched on the main thread only. The foreground package is never stored or logged.
    private var lockState = LockState.Unlocked
    private var blockList = BlockList()
    private var lastForeground: String? = null
    private var retriesLeft = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            DataStoreBlockListRepository(stadSettings).blockList.collect {
                blockList = it
                lastForeground?.let(::evaluate)
            }
        }
        scope.launch {
            LockStateRepository(stadSettings).lockState.collect { state ->
                val justLocked = !lockState.isLocked && state.isLocked
                lockState = state
                // Locking while a blocked app is open blocks it at once, if Stad has seen it.
                if (justLocked) lastForeground?.let(::evaluate)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // Only full-screen activities count: dialogs, popups and chat heads from a blocked app
        // mustn't send the user home while they're in another app.
        if (!packageManager.activityExists(ComponentName(pkg, cls))) return
        lastForeground = pkg
        retriesLeft = MAX_RETRIES
        evaluate(pkg)
    }

    private fun evaluate(pkg: String) {
        handler.removeCallbacks(retry)
        // Cheap pre-check: only a listed app while locked can be blocked.
        if (!lockState.isLocked || pkg !in blockList) {
            guard.onAllowed(pkg)
            return
        }
        // Read protection fresh for every candidate (they're rare), so a launcher, dialer, SMS app
        // or keyboard the user just switched to is never blocked, and one that stopped being
        // protected is blocked again.
        val protection = protectedPackages.current()
        if (!protection.complete) {
            // Not sure this app isn't protected, so don't block it; try again shortly.
            handler.postDelayed(retry, RETRY_DELAY_MS)
            return
        }
        if (BlockDecision.decide(pkg, lockState, blockList, protection.packages) !is Decision.Block) {
            guard.onAllowed(pkg)
            return
        }
        if (!guard.shouldLaunch(pkg, SystemClock.elapsedRealtime())) return
        block(pkg)
    }

    /** Re-checks the app Stad last saw, e.g. after an unsure protection lookup or a failed block. */
    private val retry = Runnable {
        if (retriesLeft-- > 0) lastForeground?.let(::evaluate)
    }

    private fun block(pkg: String) {
        // Home first, so the app is off-screen even if the block screen is slow. If Home was
        // refused, show the block screen straight away instead.
        val wentHome = performGlobalAction(GLOBAL_ACTION_HOME)
        // Let the home screen settle before starting the block screen on top of it, so the
        // launcher doesn't stop (and so close) the block screen straight away.
        handler.postDelayed({ showBlockScreen(pkg) }, if (wentHome) BLOCK_SCREEN_DELAY_MS else 0)
        // If neither worked, the blocked app is still the last one seen: check it again.
        handler.postDelayed({ if (lastForeground == pkg) retry.run() }, RETRY_DELAY_MS)
    }

    private fun showBlockScreen(pkg: String) {
        if (!lockState.isLocked || pkg !in blockList) return // unlocked meanwhile
        val intent = Intent(this, BlockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(BlockedActivity.EXTRA_PACKAGE, pkg)
        try {
            startActivity(intent)
        } catch (_: RuntimeException) {
            // Refused by the system; the follow-up check in block() tries again.
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val BLOCK_SCREEN_DELAY_MS = 250L
        const val RETRY_DELAY_MS = 1_500L
        const val MAX_RETRIES = 5
    }
}
