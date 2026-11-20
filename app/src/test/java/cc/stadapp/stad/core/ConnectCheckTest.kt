package cc.stadapp.stad.core

import cc.stadapp.stad.core.ConnectCheck.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectCheckTest {

    private fun decide(
        lock: LockState = LockState.Locked,
        empty: Boolean = false,
        known: Boolean = false,
        homeSent: Boolean = false,
    ) = ConnectCheck.decide(lock, empty, known, homeSent)

    @Test
    fun lockedWithUnknownForegroundGoesHome() {
        assertEquals(Action.GoHome, decide())
    }

    @Test
    fun knownForegroundIsCheckedInstead() {
        assertEquals(Action.EvaluateForeground, decide(known = true))
    }

    @Test
    fun unlockedDoesNothing() {
        assertEquals(Action.Nothing, decide(lock = LockState.Unlocked))
        assertEquals(Action.Nothing, decide(lock = LockState.Unlocked, known = true))
    }

    @Test
    fun emptyBlockListDoesNothing() {
        assertEquals(Action.Nothing, decide(empty = true))
    }

    @Test
    fun goesHomeOnlyOnce() {
        assertEquals(Action.Nothing, decide(homeSent = true))
    }

    @Test
    fun readErrorThenLockedStillGoesHome() {
        // A storage read error first reads as Unlocked; the real Locked arrives later.
        assertEquals(Action.Nothing, decide(lock = LockState.Unlocked))
        assertEquals(Action.GoHome, decide(lock = LockState.Locked))
    }

    @Test
    fun listArrivingAfterLockStillGoesHome() {
        assertEquals(Action.Nothing, decide(empty = true))
        assertEquals(Action.GoHome, decide(empty = false))
    }
}
