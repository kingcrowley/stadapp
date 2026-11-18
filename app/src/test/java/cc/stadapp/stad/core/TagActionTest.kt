package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TagActionTest {

    @Test
    fun pairedTagLocksWhenUnlocked() {
        assertEquals(TagOutcome.Lock, TagAction.decide("04A1", "04A1", LockState.Unlocked))
    }

    @Test
    fun pairedTagUnlocksWhenLocked() {
        assertEquals(TagOutcome.Unlock, TagAction.decide("04A1", "04A1", LockState.Locked))
    }

    @Test
    fun matchIgnoresFormatting() {
        assertEquals(TagOutcome.Lock, TagAction.decide("04:a1", "04A1", LockState.Unlocked))
    }

    @Test
    fun otherTagChangesNothing() {
        assertEquals(TagOutcome.NotYourTag, TagAction.decide("0499", "04A1", LockState.Locked))
        assertEquals(TagOutcome.NotYourTag, TagAction.decide(null, "04A1", LockState.Locked))
    }

    @Test
    fun noPairedTagNeverLocks() {
        for (state in LockState.entries) {
            for (scanned in listOf("04A1", null, "")) {
                val outcome = TagAction.decide(scanned, null, state)
                assertEquals(TagOutcome.NotPaired, outcome)
                assertNotEquals(TagOutcome.Lock, outcome)
            }
        }
        assertEquals(TagOutcome.NotPaired, TagAction.decide("04A1", "  ", LockState.Unlocked))
    }
}
