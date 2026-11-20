package cc.stadapp.stad.core

/** The confirmation shown and felt after a tap. The UI maps [message] to a string. */
data class TagFeedback(val message: Message, val haptic: Haptic) {

    enum class Message {
        Locked,
        LockedServiceOff,
        LockedNoApps,
        Unlocked,
        NotYourTag,
        NotPaired,
        WriteFailed,
    }

    enum class Haptic { One, Two, Three, None }

    companion object {
        /** [outcome] is null when the lock write failed: nothing changed. */
        fun of(outcome: TagOutcome?, blockedCount: Int, serviceOn: Boolean): TagFeedback = when (outcome) {
            null -> TagFeedback(Message.WriteFailed, Haptic.Three)
            TagOutcome.Lock -> TagFeedback(
                when {
                    !serviceOn -> Message.LockedServiceOff
                    blockedCount == 0 -> Message.LockedNoApps
                    else -> Message.Locked
                },
                Haptic.One,
            )
            TagOutcome.Unlock -> TagFeedback(Message.Unlocked, Haptic.Two)
            TagOutcome.NotYourTag -> TagFeedback(Message.NotYourTag, Haptic.Three)
            TagOutcome.NotPaired -> TagFeedback(Message.NotPaired, Haptic.None)
        }
    }
}
