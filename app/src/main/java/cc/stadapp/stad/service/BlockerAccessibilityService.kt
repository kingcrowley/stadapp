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
import kotlinx.coroutines.withContext

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

    // All touched on the main thread only.
    private var lockState = LockState.Unlocked
    private var blockList = BlockList()
    private var lastCompleteProtection: Set<String>? = null
    private var lastForeground: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            withContext(Dispatchers.IO) { protectedPackages.current() }.let(::rememberProtection)
        }
        scope.launch {
            DataStoreBlockListRepository(stadSettings).blockList.collect { blockList = it }
        }
        scope.launch {
            LockStateRepository(stadSettings).lockState.collect { state ->
                val justLocked = !lockState.isLocked && state.isLocked
                lockState = state
                // Locking while a blocked app is open blocks it at once.
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
        evaluate(pkg)
    }

    private fun evaluate(pkg: String) {
        if (decide(pkg) !is Decision.Block) {
            guard.onAllowed(pkg)
            return
        }
        // Blocks are rare, so re-read protection now: a launcher, dialer, SMS app or keyboard
        // the user just switched to must never be blocked.
        rememberProtection(protectedPackages.current())
        if (decide(pkg) !is Decision.Block) return
        if (!guard.shouldLaunch(pkg, SystemClock.elapsedRealtime())) return
        block(pkg)
    }

    private fun decide(pkg: String) = BlockDecision.decide(pkg, lockState, blockList, lastCompleteProtection)

    /** Keeps the last complete protected set; an incomplete one only ever adds to it. */
    private fun rememberProtection(result: ProtectedPackages.Result) {
        lastCompleteProtection = when {
            result.complete -> result.packages
            else -> lastCompleteProtection?.plus(result.packages)
        }
    }

    private fun block(pkg: String) {
        // Home first, so the app is off-screen even if the block screen is slow or refused.
        performGlobalAction(GLOBAL_ACTION_HOME)
        // Let the home screen settle before starting the block screen on top of it, so the
        // launcher doesn't stop (and so finish) the block screen straight away.
        handler.postDelayed({
            if (decide(pkg) !is Decision.Block) return@postDelayed // unlocked meanwhile
            val intent = Intent(this, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(BlockedActivity.EXTRA_PACKAGE, pkg)
            startActivity(intent)
        }, BLOCK_SCREEN_DELAY_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val BLOCK_SCREEN_DELAY_MS = 250L
    }
}
