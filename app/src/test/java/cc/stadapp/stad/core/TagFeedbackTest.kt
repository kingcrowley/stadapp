package cc.stadapp.stad.core

import cc.stadapp.stad.core.TagFeedback.Haptic
import cc.stadapp.stad.core.TagFeedback.Message
import org.junit.Assert.assertEquals
import org.junit.Test

class TagFeedbackTest {

    @Test
    fun lockWithAppsAndServiceOn() {
        assertEquals(TagFeedback(Message.Locked, Haptic.One), TagFeedback.of(TagOutcome.Lock, 3, serviceOn = true))
    }

    @Test
    fun lockWithServiceOffSaysSo() {
        assertEquals(Message.LockedServiceOff, TagFeedback.of(TagOutcome.Lock, 3, serviceOn = false).message)
    }

    @Test
    fun lockWithNoAppsSaysSo() {
        assertEquals(Message.LockedNoApps, TagFeedback.of(TagOutcome.Lock, 0, serviceOn = true).message)
    }

    @Test
    fun unlockBuzzesTwice() {
        assertEquals(TagFeedback(Message.Unlocked, Haptic.Two), TagFeedback.of(TagOutcome.Unlock, 3, true))
    }

    @Test
    fun wrongTagBuzzesThreeTimes() {
        assertEquals(TagFeedback(Message.NotYourTag, Haptic.Three), TagFeedback.of(TagOutcome.NotYourTag, 3, true))
    }

    @Test
    fun failedWriteSaysNothingChanged() {
        assertEquals(TagFeedback(Message.WriteFailed, Haptic.Three), TagFeedback.of(null, 3, true))
    }

    @Test
    fun notPairedIsQuiet() {
        assertEquals(TagFeedback(Message.NotPaired, Haptic.None), TagFeedback.of(TagOutcome.NotPaired, 3, true))
    }
}
