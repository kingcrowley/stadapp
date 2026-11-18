package cc.stadapp.stad.core

/**
 * The MIME type of the record Stad writes first on a paired tag. It must match the `TagActivity`
 * NDEF_DISCOVERED filter in the manifest.
 *
 * Not a web link: from Android 16 a tag holding an http(s) URI dispatches ACTION_VIEW instead of
 * NDEF_DISCOVERED (and from 17 only after the user taps an "open link" notification), which would
 * stop a tap reaching Stad. MIME records keep NDEF_DISCOVERED dispatch.
 */
object StadTagMime {
    const val TYPE = "application/vnd.cc.stadapp.stad"

    /** Exact match, ignoring case (MIME types are case-insensitive; Android lowercases them). */
    fun matches(type: String?): Boolean = type != null && type.equals(TYPE, ignoreCase = true)
}
