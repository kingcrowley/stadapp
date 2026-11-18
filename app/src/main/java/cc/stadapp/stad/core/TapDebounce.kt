package cc.stadapp.stad.core

/**
 * Ignores the same tag tapped again within [windowMs], so a flaky reader that sees one tap twice
 * doesn't lock and unlock straight away. Each tag has its own window, so another tag tapped in
 * between doesn't reset it. Keep one instance per process: each tap is a new activity.
 */
class TapDebounce(private val windowMs: Long = 1_500) {

    private val lastAccepted = HashMap<String, Long>()

    /** True if this tap should be acted on. */
    @Synchronized
    fun accept(id: String, nowMs: Long): Boolean {
        // Forget taps outside the window (or from a clock that went backwards), so the map stays tiny.
        lastAccepted.entries.removeAll { nowMs - it.value !in 0 until windowMs }
        if (id in lastAccepted) return false
        lastAccepted[id] = nowMs
        return true
    }
}
