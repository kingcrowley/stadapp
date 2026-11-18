package cc.stadapp.stad.core

/**
 * Ignores the same tag tapped again within [windowMs], so a flaky reader that sees one tap twice
 * doesn't lock and unlock straight away. Keep one instance per process: each tap is a new activity.
 */
class TapDebounce(private val windowMs: Long = 1_500) {

    private var lastId: String? = null
    private var lastAtMs: Long = 0

    /** True if this tap should be acted on. */
    @Synchronized
    fun accept(id: String, nowMs: Long): Boolean {
        if (id == lastId && nowMs - lastAtMs in 0 until windowMs) return false
        lastId = id
        lastAtMs = nowMs
        return true
    }
}
