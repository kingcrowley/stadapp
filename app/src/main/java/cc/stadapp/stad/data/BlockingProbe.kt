package cc.stadapp.stad.data

/**
 * Reads whether blocking is working right now. Repositories call it inside their storage edit, so
 * the history sees the state at commit time, not a value that went stale while the edit waited
 * (e.g. the service connecting in between).
 */
interface BlockingProbe {
    /** Whether the enabled-services setting lists Stad's blocking service. */
    fun serviceOn(): Boolean

    /** When Android last recorded a force-stop of Stad, if known. Read only when needed. */
    fun forceStoppedAt(): Long?

    /** A fixed answer: tests, and callers with nothing live to read. */
    data class Fixed(val on: Boolean = true, val stoppedAt: Long? = null) : BlockingProbe {
        override fun serviceOn() = on
        override fun forceStoppedAt() = stoppedAt
    }
}
