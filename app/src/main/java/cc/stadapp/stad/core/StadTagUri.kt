package cc.stadapp.stad.core

/**
 * The URI Stad writes as the first record on a paired tag. It must match the `TagActivity`
 * NDEF_DISCOVERED filter in the manifest exactly (scheme https, host stadapp.cc, path /t).
 */
object StadTagUri {
    const val VALUE = "https://stadapp.cc/t"

    /** Exact match, ignoring the case of the scheme and host only (Stad writes the record itself). */
    fun matches(uri: String?): Boolean {
        if (uri == null) return false
        val split = uri.indexOf("://")
        if (split < 0) return false
        val scheme = uri.substring(0, split)
        val rest = uri.substring(split + 3)
        val slash = rest.indexOf('/')
        if (slash < 0) return false
        val host = rest.substring(0, slash)
        val path = rest.substring(slash)
        return scheme.equals("https", ignoreCase = true) &&
            host.equals("stadapp.cc", ignoreCase = true) &&
            path == "/t"
    }
}
