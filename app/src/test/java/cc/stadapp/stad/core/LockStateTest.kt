package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockStateTest {

    @Test
    fun unlockedIsNotLocked() {
        assertFalse(LockState.Unlocked.isLocked)
    }

    @Test
    fun lockedIsLocked() {
        assertTrue(LockState.Locked.isLocked)
    }

    @Test
    fun toggleFromUnlockedLocks() {
        assertEquals(LockState.Locked, LockState.Unlocked.toggled())
    }

    @Test
    fun toggleFromLockedUnlocks() {
        assertEquals(LockState.Unlocked, LockState.Locked.toggled())
    }

    @Test
    fun toggleTwiceReturnsToStart() {
        LockState.entries.forEach { assertEquals(it, it.toggled().toggled()) }
    }
}
