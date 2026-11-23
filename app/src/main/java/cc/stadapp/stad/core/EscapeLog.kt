package cc.stadapp.stad.core

/** One stretch of time when blocking was off while Stad was locked. Times are wall-clock millis. */
data class EscapeEntry(
    val start: Long,
    val end: Long,
    /** The start is an estimate: Stad wasn't running when blocking stopped. */
    val startApprox: Boolean,
    val endedBy: EndedBy,
) {
    enum class EndedBy { ServiceBack, Unlock }

    /** Never negative, even if the clock was changed in between. */
    val durationMs: Long get() = (end - start).coerceAtLeast(0)
}

/**
 * The blocking history: when blocking was off while locked. Newest last, at most [MAX] entries.
 * Kept on the phone only; a personal record, not evidence (a clock change can make it look odd).
 */
data class EscapeLog(val entries: List<EscapeEntry> = emptyList()) {

    fun add(entry: EscapeEntry): EscapeLog = EscapeLog((entries + entry).takeLast(MAX))

    /** Entries that ended within the last [days] days, newest first. */
    fun recent(now: Long, days: Int = 7): List<EscapeEntry> {
        val since = now - days * DAY_MS
        return entries.filter { it.end >= since }.asReversed()
    }

    fun encode(): String = VERSION + entries.joinToString(ENTRY_SEP) {
        listOf(it.start, it.end, if (it.startApprox) 1 else 0, if (it.endedBy == EscapeEntry.EndedBy.Unlock) "U" else "S")
            .joinToString(FIELD_SEP)
    }

    companion object {
        const val MAX = 50
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val VERSION = "v1;"
        private const val ENTRY_SEP = "|"
        private const val FIELD_SEP = ","

        /** A missing, corrupt or unknown-version value reads as an empty log. */
        fun decode(stored: String?): EscapeLog {
            if (stored == null || !stored.startsWith(VERSION)) return EscapeLog()
            val body = stored.removePrefix(VERSION)
            if (body.isEmpty()) return EscapeLog()
            val entries = body.split(ENTRY_SEP).map { raw ->
                val f = raw.split(FIELD_SEP)
                if (f.size != 4) return EscapeLog()
                val start = f[0].toLongOrNull() ?: return EscapeLog()
                val end = f[1].toLongOrNull() ?: return EscapeLog()
                val approx = when (f[2]) { "1" -> true; "0" -> false; else -> return EscapeLog() }
                val endedBy = when (f[3]) {
                    "U" -> EscapeEntry.EndedBy.Unlock
                    "S" -> EscapeEntry.EndedBy.ServiceBack
                    else -> return EscapeLog()
                }
                EscapeEntry(start, end, approx, endedBy)
            }
            return EscapeLog(entries.takeLast(MAX))
        }
    }
}
