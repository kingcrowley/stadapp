package cc.stadapp.stad.core

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryDaysTest {

    private val zone = ZoneId.of("Europe/Dublin")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun sameDayAndYesterday() {
        assertEquals(0L, HistoryDays.ago(at(2026, 11, 21, 1), at(2026, 11, 21, 23), zone))
        assertEquals(1L, HistoryDays.ago(at(2026, 11, 20, 23), at(2026, 11, 21, 1), zone))
        assertEquals(6L, HistoryDays.ago(at(2026, 11, 15), at(2026, 11, 21), zone))
    }

    @Test
    fun acrossTheNewYear() {
        assertEquals(1L, HistoryDays.ago(at(2025, 12, 31, 23), at(2026, 1, 1, 0), zone))
    }

    @Test
    fun acrossALeapDay() {
        assertEquals(1L, HistoryDays.ago(at(2028, 2, 28), at(2028, 2, 29), zone))
        assertEquals(1L, HistoryDays.ago(at(2028, 2, 29), at(2028, 3, 1), zone))
        assertEquals(2L, HistoryDays.ago(at(2028, 2, 28), at(2028, 3, 1), zone))
    }
}
