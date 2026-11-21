package cc.stadapp.stad.ui

import android.content.Context
import android.text.format.DateFormat
import cc.stadapp.stad.R
import cc.stadapp.stad.core.EscapeEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Words for one blocking-history entry, e.g. "Today 14:02 – 14:20 (18 min)". */
object HistoryText {

    fun line(context: Context, entry: EscapeEntry, now: Long): String {
        val day = dayLabel(context, entry.start, now)
        val start = time(context, entry.start)
        val end = time(context, entry.end)
        return when {
            !entry.startApprox -> context.getString(R.string.history_exact, day, start, end, duration(context, entry.durationMs))
            entry.endedBy == EscapeEntry.EndedBy.Unlock -> context.getString(R.string.history_approx_unlock, day, start, end)
            else -> context.getString(R.string.history_approx, day, start, end)
        }
    }

    private fun time(context: Context, at: Long): String = DateFormat.getTimeFormat(context).format(Date(at))

    private fun dayLabel(context: Context, at: Long, now: Long): String {
        val days = dayNumber(now) - dayNumber(at)
        return when (days) {
            0L -> context.getString(R.string.history_today)
            1L -> context.getString(R.string.history_yesterday)
            else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(at))
        }
    }

    private fun dayNumber(at: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = at }
        return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun duration(context: Context, ms: Long): String {
        val minutes = ms / 60_000
        return when {
            minutes < 1 -> context.getString(R.string.history_under_a_minute)
            minutes < 60 -> context.resources.getQuantityString(R.plurals.history_minutes, minutes.toInt(), minutes.toInt())
            else -> context.getString(R.string.history_hours, (minutes / 60).toInt(), (minutes % 60).toInt())
        }
    }
}
