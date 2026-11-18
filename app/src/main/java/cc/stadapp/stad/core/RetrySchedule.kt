package cc.stadapp.stad.core

/**
 * How long to wait before checking a blocked app again, when a check couldn't finish (protection
 * unknown) or a block didn't take (Home and the block screen both refused).
 *
 * The first [fastTries] retries come quickly; after that Stad keeps trying at a slow, bounded rate
 * for as long as it's needed, so a blocked app is never left usable just because the retries ran
 * out. Call [reset] when a new app comes to the front.
 */
class RetrySchedule(
    private val fastTries: Int = 5,
    private val fastDelayMs: Long = 1_500,
    private val slowDelayMs: Long = 10_000,
) {
    private var used = 0

    fun reset() {
        used = 0
    }

    /** The delay before the next retry. Never runs out. */
    fun nextDelayMs(): Long = if (used < fastTries) {
        used++
        fastDelayMs
    } else {
        slowDelayMs
    }
}
