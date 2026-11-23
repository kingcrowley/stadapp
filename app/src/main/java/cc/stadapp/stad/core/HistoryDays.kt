package cc.stadapp.stad.core

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Calendar days between two instants in a time zone: 0 is today, 1 yesterday. */
object HistoryDays {
    fun ago(at: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        ChronoUnit.DAYS.between(date(at, zone), date(now, zone))

    private fun date(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
