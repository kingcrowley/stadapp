package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RetryScheduleTest {

    private val schedule = RetrySchedule(fastTries = 5, fastDelayMs = 1_500, slowDelayMs = 10_000)

    @Test
    fun firstRetriesAreFast() {
        repeat(5) { assertEquals(1_500L, schedule.nextDelayMs()) }
    }

    @Test
    fun keepsRetryingSlowlyAfterTheFastRetriesRunOut() {
        repeat(5) { schedule.nextDelayMs() }
        // Repeated failures with no new event: retries never stop, they just slow down.
        repeat(100) { assertEquals(10_000L, schedule.nextDelayMs()) }
    }

    @Test
    fun resetStartsFastAgain() {
        repeat(20) { schedule.nextDelayMs() }
        schedule.reset()
        assertEquals(1_500L, schedule.nextDelayMs())
    }
}
