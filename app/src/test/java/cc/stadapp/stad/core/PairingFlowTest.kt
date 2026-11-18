package cc.stadapp.stad.core

import cc.stadapp.stad.core.PairingFlow.Error
import cc.stadapp.stad.core.PairingFlow.State
import cc.stadapp.stad.core.PairingFlow.Step
import cc.stadapp.stad.core.PairingFlow.WriteOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PairingFlowTest {

    private val ntag = byteArrayOf(0x04, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66)
    private val other = byteArrayOf(0x04, 0x11, 0x22, 0x33, 0x44, 0x55, 0x77)
    private val ntagId = "04112233445566"

    private fun tap(
        state: State,
        uid: ByteArray = ntag,
        write: WriteOutcome = WriteOutcome.Written,
        verify: Boolean = true,
    ) = PairingFlow.onTap(state, uid, { write }, { verify })

    @Test
    fun twoGoodTapsReachSaving() {
        val first = tap(State())
        assertEquals(State(Step.Confirming(ntagId)), first)
        assertEquals(State(Step.Saving(ntagId)), tap(first))
    }

    @Test
    fun savedTagIsDone() {
        assertEquals(State(Step.Done(ntagId)), PairingFlow.afterSave(State(Step.Saving(ntagId)), saved = true))
    }

    @Test
    fun refusedSaveMeansLocked() {
        assertEquals(State(Step.Waiting, Error.Locked), PairingFlow.afterSave(State(Step.Saving(ntagId)), saved = false))
    }

    @Test
    fun failedSaveCanBeRetriedWithAnotherTap() {
        val after = PairingFlow.afterSave(State(Step.Saving(ntagId)), saved = null)
        assertEquals(State(Step.Confirming(ntagId), Error.SaveFailed), after)
        assertEquals(State(Step.Saving(ntagId)), tap(after))
    }

    @Test
    fun randomIdIsRefusedWithoutWriting() {
        var wrote = false
        val state = PairingFlow.onTap(State(), byteArrayOf(0x08, 1, 2, 3), { wrote = true; WriteOutcome.Written }, { true })
        assertEquals(State(Step.Waiting, Error.RandomId), state)
        assertFalse(wrote)
    }

    @Test
    fun unwritableTagIsRefused() {
        assertEquals(State(Step.Waiting, Error.CantWrite), tap(State(), write = WriteOutcome.CantWrite))
    }

    @Test
    fun failedWriteWaitsForAnotherTry() {
        assertEquals(State(Step.Waiting, Error.WriteFailed), tap(State(), write = WriteOutcome.Failed))
    }

    @Test
    fun differentSecondTagStartsAgain() {
        assertEquals(State(Step.Waiting, Error.DifferentTag), tap(State(Step.Confirming(ntagId)), uid = other))
    }

    @Test
    fun brokenRecordOnSecondTapAsksForAnotherTap() {
        val confirming = State(Step.Confirming(ntagId))
        assertEquals(confirming.copy(error = Error.WriteFailed), tap(confirming, verify = false))
    }

    @Test
    fun secondTapDoesNotWriteAgain() {
        var wrote = false
        PairingFlow.onTap(State(Step.Confirming(ntagId)), ntag, { wrote = true; WriteOutcome.Written }, { true })
        assertFalse(wrote)
    }

    @Test
    fun tapsWhileSavingOrDoneAreIgnored() {
        assertEquals(State(Step.Saving(ntagId)), tap(State(Step.Saving(ntagId)), uid = other))
        assertEquals(State(Step.Done(ntagId)), tap(State(Step.Done(ntagId)), uid = other))
    }

    @Test
    fun emptyUidIsAFailedRead() {
        assertEquals(State(Step.Waiting, Error.WriteFailed), tap(State(), uid = byteArrayOf()))
    }
}
