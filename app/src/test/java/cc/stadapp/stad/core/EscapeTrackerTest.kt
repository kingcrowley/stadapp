package cc.stadapp.stad.core

import cc.stadapp.stad.core.EscapeEntry.EndedBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscapeTrackerTest {

    private val min = 60_000L
    private val locked = EscapeState(lockedAt = 0, lastAlive = 10 * min, serviceConnected = true, connectedAt = 0)

    @Test
    fun turnedOffThenOnIsRecordedExactly() {
        val off = EscapeTracker.onServiceStopped(locked, locked = true, settingOn = false, now = 20 * min).state
        assertEquals(20 * min, off.offSince)
        val back = EscapeTracker.onServiceConnected(off, locked = true, forceStoppedAt = null, now = 38 * min)
        assertEquals(EscapeEntry(20 * min, 38 * min, false, EndedBy.ServiceBack), back.entry)
        assertNull(back.state.offSince)
    }

    @Test
    fun systemUnbindWithSettingStillOnIsNotRecorded() {
        val r = EscapeTracker.onServiceStopped(locked, locked = true, settingOn = true, now = 20 * min)
        assertNull(r.state.offSince)
        assertFalse(r.state.serviceConnected)
        // Reboot: reconnects with nothing to close.
        assertNull(EscapeTracker.onServiceConnected(r.state, true, null, 25 * min).entry)
    }

    @Test
    fun unlockedRecordsNothing() {
        val unlocked = locked.copy(lockedAt = null)
        assertNull(EscapeTracker.onServiceStopped(unlocked, locked = false, settingOn = false, now = 5).state.offSince)
        assertNull(EscapeTracker.onFoundOff(unlocked, locked = false, forceStoppedAt = null, now = 5).state.offSince)
    }

    @Test
    fun doubleStopKeepsTheFirstTime() {
        val once = EscapeTracker.onServiceStopped(locked, true, false, 20 * min).state
        val twice = EscapeTracker.onServiceStopped(once, true, false, 21 * min).state
        assertEquals(20 * min, twice.offSince)
    }

    @Test
    fun exactStopReplacesAnEstimate() {
        val found = EscapeTracker.onFoundOff(locked, true, null, 20 * min).state
        assertTrue(found.offSinceApprox)
        val exact = EscapeTracker.onServiceStopped(found, true, false, 20 * min + 500).state
        assertEquals(20 * min + 500, exact.offSince)
        assertFalse(exact.offSinceApprox)
    }

    @Test
    fun foundOffUsesForceStopTimeWhenKnown() {
        val s = EscapeTracker.onFoundOff(locked, true, forceStoppedAt = 15 * min, now = 30 * min).state
        assertEquals(15 * min, s.offSince)
        assertFalse(s.offSinceApprox)
    }

    @Test
    fun foundOffEstimatesFromLastAliveButNeverBeforeTheLock() {
        assertEquals(10 * min, EscapeTracker.onFoundOff(locked, true, null, 30 * min).state.offSince)
        // last_alive from an earlier lock days ago: capped at this lock.
        val stale = locked.copy(lastAlive = 1, lockedAt = 25 * min)
        val s = EscapeTracker.onFoundOff(stale, true, forceStoppedAt = 2, now = 30 * min).state
        assertEquals(25 * min, s.offSince)
        assertTrue(s.offSinceApprox)
    }

    @Test
    fun aForceStopFromBeforeTheLastConnectionIsNotReused() {
        // Force-stopped at 12 min, re-enabled at 20 (recorded then), turned off at 40, and
        // Home's found-off check gets in before the stop write: 12 min belongs to the old period.
        val again = EscapeTracker.onServiceConnected(locked, true, forceStoppedAt = 12 * min, now = 20 * min).state
        val found = EscapeTracker.onFoundOff(again.copy(serviceConnected = false), true, forceStoppedAt = 12 * min, now = 40 * min).state
        assertEquals(20 * min, found.offSince)
        assertTrue(found.offSinceApprox)
        // The exact stop then replaces the estimate.
        assertEquals(40 * min, EscapeTracker.onServiceStopped(found, true, false, 40 * min).state.offSince)
    }

    @Test
    fun killedAndReenabledWithoutStadRunningIsRecorded() {
        // Force-stopped at 12 min; nothing ran; user turned it back on from Settings at 30 min.
        val r = EscapeTracker.onServiceConnected(locked, true, forceStoppedAt = 12 * min, now = 30 * min)
        assertEquals(EscapeEntry(12 * min, 30 * min, false, EndedBy.ServiceBack), r.entry)
    }

    @Test
    fun crashOrUpdateRestartIsNotRecorded() {
        // No force-stop since the last connect.
        assertNull(EscapeTracker.onServiceConnected(locked, true, forceStoppedAt = null, now = 30 * min).entry)
        val oldStop = locked.copy(connectedAt = 5 * min)
        assertNull(EscapeTracker.onServiceConnected(oldStop, true, forceStoppedAt = 4 * min, now = 30 * min).entry)
    }

    @Test
    fun quickToggleIsDropped() {
        val off = EscapeTracker.onServiceStopped(locked, true, false, 20 * min).state
        assertNull(EscapeTracker.onServiceConnected(off, true, null, 20 * min + 3_000).entry)
    }

    @Test
    fun lockingWhileOffStartsAtTheLock() {
        val s = EscapeTracker.onLocked(EscapeState(lastAlive = 1), serviceOn = false, now = 50 * min).state
        assertEquals(50 * min, s.offSince)
        assertFalse(s.offSinceApprox)
        assertNull(EscapeTracker.onLocked(s, serviceOn = true, now = 60 * min).state.offSince)
    }

    @Test
    fun unlockAfterForceStopRecordsFoundOffAndEndsIt() {
        val r = EscapeTracker.onUnlocked(locked, serviceOn = false, forceStoppedAt = 15 * min, now = 40 * min)
        assertEquals(EscapeEntry(15 * min, 40 * min, false, EndedBy.Unlock), r.entry)
        assertNull(r.state.offSince)
        assertNull(r.state.lockedAt)
    }

    @Test
    fun unlockWithBlockingOnRecordsNothing() {
        assertNull(EscapeTracker.onUnlocked(locked, serviceOn = true, forceStoppedAt = null, now = 40 * min).entry)
    }

    @Test
    fun clockGoingBackwardsNeverGivesANegativeDuration() {
        val off = EscapeTracker.onServiceStopped(locked, true, false, 20 * min).state
        val r = EscapeTracker.onServiceConnected(off, true, null, 2 * min)
        assertNull(r.entry) // clamped to 0, so it's under the toggle threshold
    }

    @Test
    fun aliveIsTouchedSparingly() {
        assertTrue(EscapeTracker.shouldTouchAlive(EscapeState(), locked = true, now = 0))
        assertFalse(EscapeTracker.shouldTouchAlive(locked, locked = true, now = 11 * min))
        assertTrue(EscapeTracker.shouldTouchAlive(locked, locked = true, now = 25 * min))
        assertFalse(EscapeTracker.shouldTouchAlive(locked, locked = false, now = 99 * min))
    }
}
