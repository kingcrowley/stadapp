package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockingHealthTest {

    @Test
    fun serviceOnIsOkWhateverTheLock() {
        assertEquals(BlockingHealth.Ok, BlockingHealth.of(LockState.Locked, serviceEnabled = true))
        assertEquals(BlockingHealth.Ok, BlockingHealth.of(LockState.Unlocked, serviceEnabled = true))
    }

    @Test
    fun serviceOffWhileLockedIsTheWarning() {
        assertEquals(BlockingHealth.OffWhileLocked, BlockingHealth.of(LockState.Locked, serviceEnabled = false))
    }

    @Test
    fun serviceOffWhileUnlockedIsNotAWarning() {
        assertEquals(BlockingHealth.OffUnlocked, BlockingHealth.of(LockState.Unlocked, serviceEnabled = false))
    }
}
