package cc.stadapp.stad.core

/**
 * A tag's identity: its UID as uppercase hex with no separators, e.g. "04A1B2C3D4E5F6".
 * Stored on the device only and never logged.
 */
object TagId {

    fun fromBytes(bytes: ByteArray): String? =
        if (bytes.isEmpty()) null else bytes.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    /** Trims, uppercases and drops `:`, `-` and spaces. Blank or non-hex input gives null. */
    fun normalize(value: String?): String? {
        val cleaned = value?.filterNot { it == ':' || it == '-' || it.isWhitespace() }?.uppercase()
        if (cleaned.isNullOrEmpty()) return null
        return cleaned.takeIf { s -> s.all { it in '0'..'9' || it in 'A'..'F' } }
    }
}
