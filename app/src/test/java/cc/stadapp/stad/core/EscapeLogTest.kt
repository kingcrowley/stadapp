package cc.stadapp.stad.core

import cc.stadapp.stad.core.EscapeEntry.EndedBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EscapeLogTest {

    private val day = 24 * 60 * 60 * 1000L
    private fun entry(start: Long, end: Long = start + 1000, approx: Boolean = false, by: EndedBy = EndedBy.ServiceBack) =
        EscapeEntry(start, end, approx, by)

    @Test
    fun capDropsTheOldest() {
        var log = EscapeLog()
        repeat(EscapeLog.MAX + 3) { log = log.add(entry(it.toLong())) }
        assertEquals(EscapeLog.MAX, log.entries.size)
        assertEquals(3L, log.entries.first().start)
    }

    @Test
    fun recentKeepsTheLastSevenDaysNewestFirst() {
        val now = 100 * day
        val log = EscapeLog()
            .add(entry(now - 8 * day, end = now - 7 * day - 1))
            .add(entry(now - 7 * day - 10, end = now - 7 * day))
            .add(entry(now - day))
        assertEquals(listOf(now - day, now - 7 * day - 10), log.recent(now).map { it.start })
    }

    @Test
    fun roundTrips() {
        val log = EscapeLog().add(entry(1, 2)).add(entry(3, 400, approx = true, by = EndedBy.Unlock))
        assertEquals(log, EscapeLog.decode(log.encode()))
        assertEquals(EscapeLog(), EscapeLog.decode(EscapeLog().encode()))
    }

    @Test
    fun corruptOrUnknownReadsAsEmpty() {
        listOf(null, "", "garbage", "v2;1,2,0,S", "v1;1,2,0", "v1;x,2,0,S", "v1;1,2,7,S", "v1;1,2,0,Q")
            .forEach { assertTrue(it.toString(), EscapeLog.decode(it).entries.isEmpty()) }
    }

    @Test
    fun durationNeverNegative() {
        assertEquals(0L, entry(1000, end = 10).durationMs)
    }
}
