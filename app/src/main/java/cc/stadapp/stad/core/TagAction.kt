package cc.stadapp.stad.core

/** What a tag tap does. */
enum class TagOutcome {
    /** No tag is paired, so a tap does nothing. */
    NotPaired,
    /** A different tag: nothing changes. */
    NotYourTag,
    Lock,
    Unlock,
}

object TagAction {

    /**
     * Only the paired tag changes the lock, and it toggles it. With no paired tag, nothing locks,
     * so Stad can never be locked without a tag that can unlock it.
     */
    fun decide(scannedId: String?, pairedId: String?, lockState: LockState): TagOutcome {
        val paired = TagId.normalize(pairedId) ?: return TagOutcome.NotPaired
        if (TagId.normalize(scannedId) != paired) return TagOutcome.NotYourTag
        return if (lockState.isLocked) TagOutcome.Unlock else TagOutcome.Lock
    }
}
