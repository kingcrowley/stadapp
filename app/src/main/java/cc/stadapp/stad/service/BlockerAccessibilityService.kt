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
import cc.stadapp.stad.core.ConnectCheck
import cc.stadapp.stad.core.Decision
import cc.stadapp.stad.core.EscapeTracker
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.RelaunchGuard
import cc.stadapp.stad.core.RetrySchedule
import cc.stadapp.stad.data.DataStoreBlockListRepository
import cc.stadapp.stad.data.EscapeRepository
import cc.stadapp.stad.data.LockStateRepository
import cc.stadapp.stad.data.stadSettings
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

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
    private val retries = RetrySchedule()
    private val protectedPackages by lazy { ProtectedPackages(this) }

    // All touched on the main thread only. The foreground package is never stored or logged.
    private var lockState = LockState.Unlocked
    private var blockList = BlockList()
    private var lastForeground: String? = null
    // Whether this connection has already sent the user Home (see checkOnConnect).
    private var homeSent = false
    private val escape by lazy { EscapeRepository(stadSettings) }
    // Last "seen working" write, so window events don't each start a storage edit.
    private var aliveTouchedAt = Long.MIN_VALUE / 2
    private var stopRecorded = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Connected means blocking works, so the "blocking is off" warning no longer applies.
        BlockingWarning.cancel(this)
        stopRecorded = false
        aliveTouchedAt = System.currentTimeMillis()
        scope.launch {
            // Ends an off period in the blocking history, if there was one.
            safely { escape.onServiceConnected(ForceStops.lastAt(this@BlockerAccessibilityService), aliveTouchedAt) }
        }
        scope.launch {
            DataStoreBlockListRepository(stadSettings).blockList.collect {
                blockList = it
                if (!checkOnConnect()) lastForeground?.let(::evaluate)
            }
        }
        scope.launch {
            LockStateRepository(stadSettings).lockState.collect { state ->
                val justLocked = !lockState.isLocked && state.isLocked
                lockState = state
                // Locking while a blocked app is open blocks it at once, if Stad has seen it.
                if (!checkOnConnect() && justLocked) lastForeground?.let(::evaluate)
            }
        }
    }

    /**
     * After a (re)connect, e.g. a reboot or an update of Stad, a blocked app may already be in
     * front, and Stad can't ask which app that is. So while it's unknown and Stad is locked with
     * apps to block, go Home once. Checked on every stored change, since a read error first reads
     * as Unlocked and the real lock arrives later. Returns whether it acted.
     */
    private fun checkOnConnect(): Boolean =
        when (ConnectCheck.decide(lockState, blockList.isEmpty(), lastForeground != null, homeSent)) {
            ConnectCheck.Action.GoHome -> {
                homeSent = performGlobalAction(GLOBAL_ACTION_HOME)
                // Refused: try again shortly, until Home works, a window event shows what's in
                // front, or blocking no longer applies (checkOnConnect then does nothing).
                handler.removeCallbacks(connectRetry)
                if (!homeSent) handler.postDelayed(connectRetry, CONNECT_RETRY_MS)
                true
            }
            ConnectCheck.Action.EvaluateForeground -> {
                lastForeground?.let(::evaluate)
                true
            }
            ConnectCheck.Action.Nothing -> false
        }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // Only full-screen activities count: dialogs, popups and chat heads from a blocked app
        // mustn't send the user home while they're in another app.
        if (!packageManager.activityExists(ComponentName(pkg, cls))) return
        // A tag tap's invisible screen isn't "the app in front": the app under it still is, so
        // locking with the tag over a blocked app blocks that app at once.
        if (pkg == packageName && cls == TagActivity::class.java.name) return
        lastForeground = pkg
        retries.reset()
        touchAlive()
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
            scheduleRetry()
            return
        }
        if (BlockDecision.decide(pkg, lockState, blockList, protection.packages) !is Decision.Block) {
            guard.onAllowed(pkg)
            return
        }
        if (!guard.shouldLaunch(pkg, SystemClock.elapsedRealtime())) {
            // Just blocked: don't launch again yet, but keep a check pending in case that block
            // didn't take (the removeCallbacks above may have dropped it).
            scheduleRetry()
            return
        }
        block(pkg)
    }

    private val connectRetry = Runnable { checkOnConnect() }

    /** Re-checks the app Stad last saw, e.g. after an unsure protection lookup or a failed block. */
    private val retry = Runnable { lastForeground?.let(::evaluate) }

    // Fast at first, then slow but never-ending: a blocked app is never left usable because the
    // retries ran out. It stops as soon as an allowed app is the one in front.
    private fun scheduleRetry() {
        handler.removeCallbacks(retry)
        handler.postDelayed(retry, retries.nextDelayMs())
    }

    private fun block(pkg: String) {
        // Home first, so the app is off-screen even if the block screen is slow. If Home was
        // refused, show the block screen straight away instead.
        val wentHome = performGlobalAction(GLOBAL_ACTION_HOME)
        // Let the home screen settle before starting the block screen on top of it, so the
        // launcher doesn't stop (and so close) the block screen straight away.
        handler.postDelayed({ showBlockScreen(pkg) }, if (wentHome) BLOCK_SCREEN_DELAY_MS else 0)
        // If neither worked, the blocked app is still the last one seen and gets checked again.
        scheduleRetry()
    }

    private fun showBlockScreen(pkg: String) {
        if (!lockState.isLocked || pkg !in blockList) return // unlocked meanwhile
        // If the user has moved on to another app since Home, don't cover it. The home screen
        // Stad sent them to (or the blocked app, if Home was refused) still gets the block screen.
        val now = lastForeground
        if (now != null && now != pkg && protectedPackages.isHomeApp(now) == false) return
        val intent = Intent(this, BlockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(BlockedActivity.EXTRA_PACKAGE, pkg)
        try {
            startActivity(intent)
        } catch (_: RuntimeException) {
            // Refused by the system; the follow-up check in block() tries again.
        }
    }

    /** Refreshes "last seen working" while locked, at most every 15 minutes. */
    private fun touchAlive() {
        if (!lockState.isLocked) return
        val now = System.currentTimeMillis()
        if (now - aliveTouchedAt < EscapeTracker.ALIVE_INTERVAL_MS) return
        aliveTouchedAt = now
        scope.launch { safely { escape.touchAlive(now) } }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        recordStop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        recordStop() // if onUnbind didn't run
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Notes the stop in the blocking history. Synchronous on purpose: once the service is destroyed
     * the process may die at once, and the scope is cancelled. It only counts if the setting no
     * longer lists Stad, so a shutdown or user switch isn't recorded. DataStore writes on its own
     * thread, typically in tens of milliseconds; the timeout bounds the wait.
     */
    private fun recordStop() {
        if (stopRecorded) return
        stopRecorded = true
        val settingOn = ServiceStatus.isOn(this)
        runBlocking { withTimeoutOrNull(STOP_WRITE_TIMEOUT_MS) { safely { escape.onServiceStopped(settingOn, System.currentTimeMillis()) } } }
    }

    /** History writes are best effort: a storage error must never affect blocking. */
    private suspend fun safely(write: suspend () -> Unit) {
        try {
            write()
        } catch (_: IOException) {
            // Not recorded.
        }
    }

    private companion object {
        const val BLOCK_SCREEN_DELAY_MS = 250L
        const val CONNECT_RETRY_MS = 1_000L
        const val STOP_WRITE_TIMEOUT_MS = 2_000L
    }
}
