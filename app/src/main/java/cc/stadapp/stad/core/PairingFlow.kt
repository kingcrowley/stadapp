package cc.stadapp.stad.core

/**
 * Pairing takes two taps. The first reads the tag's ID and writes Stad's record to it; the second
 * checks the ID is the same and that the record reads back intact (a tag pulled away mid-write can
 * be left half-written), rewriting it if not. Only then is the tag saved.
 *
 * Pure: the tag I/O is passed in as functions, so the steps are unit-tested on the JVM.
 */
object PairingFlow {

    sealed interface Step {
        data object Waiting : Step
        data class Confirming(val firstId: String) : Step
        /** Ready to be stored; the caller saves it and reports back with [afterSave]. */
        data class Saving(val id: String) : Step
        data class Done(val id: String) : Step
    }

    enum class Error {
        /** The tag shows a new random ID each time, so it can't identify anything. */
        RandomId,
        /** Read-only, too small or not an NDEF tag. Use another tag. */
        CantWrite,
        /** Writing or checking failed, usually because the tag moved. Tap again. */
        WriteFailed,
        /** The second tap was a different tag. Start again. */
        DifferentTag,
        /** Locked: the tag can only be changed while unlocked. */
        Locked,
        SaveFailed,
    }

    enum class WriteOutcome { Written, CantWrite, Failed }

    data class State(val step: Step = Step.Waiting, val error: Error? = null)

    /**
     * A tag was tapped. [write] writes Stad's record and is only called on the first tap;
     * [verify] checks the record reads back intact (rewriting it if needed) on the second.
     */
    fun onTap(
        state: State,
        uid: ByteArray,
        write: () -> WriteOutcome,
        verify: () -> Boolean,
    ): State {
        val id = TagId.fromBytes(uid) ?: return state.copy(error = Error.WriteFailed)
        return when (val step = state.step) {
            is Step.Confirming -> when (PairingCheck.confirm(step.firstId, id)) {
                PairingCheck.Result.Unstable -> State(Step.Waiting, Error.DifferentTag)
                PairingCheck.Result.Missing -> state.copy(error = Error.WriteFailed)
                is PairingCheck.Result.Confirmed ->
                    if (verify()) State(Step.Saving(id)) else state.copy(error = Error.WriteFailed)
            }
            // Saving or done: ignore further taps until the caller moves on.
            is Step.Saving, is Step.Done -> state
            Step.Waiting -> when {
                PairingCheck.isRandomUid(uid) -> State(Step.Waiting, Error.RandomId)
                else -> when (write()) {
                    WriteOutcome.Written -> State(Step.Confirming(id))
                    WriteOutcome.CantWrite -> State(Step.Waiting, Error.CantWrite)
                    WriteOutcome.Failed -> State(Step.Waiting, Error.WriteFailed)
                }
            }
        }
    }

    /** [saved] is true if stored, false if refused because locked, null if the write failed. */
    fun afterSave(state: State, saved: Boolean?): State {
        val step = state.step as? Step.Saving ?: return state
        return when (saved) {
            true -> State(Step.Done(step.id))
            false -> State(Step.Waiting, Error.Locked)
            null -> State(Step.Confirming(step.id), Error.SaveFailed)
        }
    }
}
