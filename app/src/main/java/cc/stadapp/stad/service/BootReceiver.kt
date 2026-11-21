package cc.stadapp.stad.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cc.stadapp.stad.core.BlockingHealth
import cc.stadapp.stad.core.BootCheck
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.data.EscapeRepository
import cc.stadapp.stad.data.LockStateRepository
import cc.stadapp.stad.data.stadSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * After a reboot or an update of Stad, warns if Stad is locked but its Accessibility service is
 * off, since then nothing is blocked. It can't turn the service on (only the user can) and needn't
 * start it: Android binds an enabled service itself. Not exported: both broadcasts are protected
 * system broadcasts, which reach it anyway.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!BootCheck.handles(intent.action)) return
        val app = context.applicationContext
        // Taken before the slow read, so Home or the service cancelling meanwhile wins.
        val token = BlockingWarning.token()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val health = BlockingHealth.of(readLock(app), ServiceStatus.isOn(app))
                if (health == BlockingHealth.OffWhileLocked) {
                    recordFoundOff(app)
                    BlockingWarning.post(app, token)
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** A slow or failed read counts as Unlocked, so no warning: Stad fails open, never trapping the user. */
    private suspend fun readLock(context: Context): LockState =
        try {
            withTimeoutOrNull(READ_TIMEOUT_MS) { LockStateRepository(context.stadSettings).lockState.first() }
        } catch (_: Exception) {
            null
        } ?: LockState.Unlocked

    /** Notes in the blocking history that blocking is off (e.g. after a force-stop). Best effort. */
    private suspend fun recordFoundOff(context: Context) {
        try {
            withTimeoutOrNull(READ_TIMEOUT_MS) {
                EscapeRepository(context.stadSettings).onFoundOff(ForceStops.lastAt(context), System.currentTimeMillis())
            }
        } catch (_: Exception) {
            // The next time Stad runs tries again.
        }
    }

    private companion object {
        // Well inside the ~10 s a goAsync() receiver gets.
        const val READ_TIMEOUT_MS = 5_000L
    }
}
