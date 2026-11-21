package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceStopRecordTest {

    private val hour = 3_600_000L

    @Test
    fun aUserRequestedExitAwayFromAnUpdateIsAForceStop() {
        assertTrue(ForceStopRecord.isForceStop(true, "stop cc.stadapp.stad due to from pid 1234", 10 * hour, 2 * hour, separatesUpdates = false))
        assertTrue(ForceStopRecord.isForceStop(true, null, 10 * hour, null, separatesUpdates = true))
    }

    @Test
    fun otherReasonsAreNot() {
        assertFalse(ForceStopRecord.isForceStop(false, null, 10 * hour, null, separatesUpdates = true))
    }

    @Test
    fun removalFromRecentsIsNot() {
        assertFalse(ForceStopRecord.isForceStop(true, "remove task", 10 * hour, null, separatesUpdates = true))
    }

    @Test
    fun beforeAndroid14AnExitAtUpdateTimeIsTheUpdate() {
        // The install stamp can land just before or just after the kill.
        assertFalse(ForceStopRecord.isForceStop(true, null, 10 * hour, 10 * hour + 20_000, separatesUpdates = false))
        assertFalse(ForceStopRecord.isForceStop(true, null, 10 * hour, 10 * hour - 20_000, separatesUpdates = false))
        // Android 14+ files updates under their own reason, so the time doesn't matter there.
        assertTrue(ForceStopRecord.isForceStop(true, null, 10 * hour, 10 * hour, separatesUpdates = true))
    }
}
