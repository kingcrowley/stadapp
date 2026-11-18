package cc.stadapp.stad.core

/** What to do when an app comes to the front. */
sealed interface Decision {
    data object Allow : Decision
    data class Block(val packageName: String) : Decision
}

object BlockDecision {

    /**
     * Whether [foregroundPackage] should be covered by the block screen.
     *
     * [protectedPackages] is the last *complete* protected set the device reported, or null if
     * there has never been one. With null, nothing is blocked: it's better to miss a block than to
     * block a launcher or dialer and trap the user. This fail-open choice is deliberate.
     */
    fun decide(
        foregroundPackage: String?,
        lockState: LockState,
        blockList: BlockList,
        protectedPackages: Set<String>?,
    ): Decision {
        if (!lockState.isLocked) return Decision.Allow
        val pkg = foregroundPackage?.takeIf { it.isNotBlank() } ?: return Decision.Allow
        if (protectedPackages == null) return Decision.Allow
        if (pkg in BlockListRules.ALWAYS_PROTECTED || pkg in protectedPackages) return Decision.Allow
        return if (pkg in blockList) Decision.Block(pkg) else Decision.Allow
    }
}
