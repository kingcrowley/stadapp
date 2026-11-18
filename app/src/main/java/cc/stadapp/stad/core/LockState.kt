package cc.stadapp.stad.core

/**
 * Whether the user's chosen apps are currently blocked.
 *
 * Pure Kotlin with no Android dependencies, so it is unit-testable on the JVM.
 * The blocking engine (1.2) reads it; slice 1.3 toggles it from an NFC tap.
 */
enum class LockState {
    Unlocked,
    Locked;

    val isLocked: Boolean get() = this == Locked

    /** The state after a tag tap: locked becomes unlocked and vice versa. */
    fun toggled(): LockState = if (isLocked) Unlocked else Locked

    companion object {
        /**
         * Reads a stored value. Missing or unknown values give [Unlocked]: failing open is
         * deliberate, so a storage fault can never trap the user.
         */
        fun fromStored(value: String?): LockState = entries.firstOrNull { it.name == value } ?: Unlocked
    }
}
