package cc.stadapp.stad.core

/**
 * Whether the user's chosen apps are currently blocked.
 *
 * Pure Kotlin with no Android dependencies, so it is unit-testable on the JVM.
 * Slice 1.2 drives the blocking engine from this; slice 1.3 toggles it from an NFC tap.
 */
enum class LockState {
    Unlocked,
    Locked;

    val isLocked: Boolean get() = this == Locked

    /** The state after a tag tap: locked becomes unlocked and vice versa. */
    fun toggled(): LockState = if (isLocked) Unlocked else Locked
}
