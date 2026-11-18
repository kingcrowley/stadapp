package cc.stadapp.stad.core

/** Checks a tag is fit to pair: its ID must stay the same from tap to tap. */
object PairingCheck {

    sealed interface Result {
        data class Confirmed(val id: String) : Result
        /** The two taps gave different IDs (a different tag, or a tag with a random ID). */
        data object Unstable : Result
        data object Missing : Result
    }

    /**
     * ISO 14443-3: a 4-byte NFC-A UID starting 0x08 is a random ID that changes every time the
     * tag is read, so it can't identify a tag.
     */
    fun isRandomUid(uid: ByteArray): Boolean = uid.size == 4 && uid[0] == 0x08.toByte()

    fun confirm(firstId: String?, secondId: String?): Result {
        val first = TagId.normalize(firstId)
        val second = TagId.normalize(secondId)
        return when {
            first == null || second == null -> Result.Missing
            first != second -> Result.Unstable
            else -> Result.Confirmed(first)
        }
    }
}
