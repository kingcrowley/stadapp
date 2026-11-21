package cc.stadapp.stad.core

/** Whether blocking actually works, given the lock and Stad's Accessibility service. */
enum class BlockingHealth {
    Ok,
    /** Service off, but nothing to block yet. */
    OffUnlocked,
    /** Locked, but nothing is being blocked: the user must turn the service back on. */
    OffWhileLocked;

    companion object {
        fun of(lockState: LockState, serviceEnabled: Boolean): BlockingHealth = when {
            serviceEnabled -> Ok
            lockState.isLocked -> OffWhileLocked
            else -> OffUnlocked
        }
    }
}
