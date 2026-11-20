package cc.stadapp.stad.service

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import cc.stadapp.stad.core.PairingFlow.WriteOutcome
import cc.stadapp.stad.core.StadTagMime
import java.io.IOException

/**
 * Writes and checks Stad's record on a tag: a Stad MIME record first (Android dispatches on the
 * first record), then an Android Application Record so a tap starts Stad. Blocking I/O: call it from the
 * NFC reader thread, never the main thread.
 */
object TagWriter {

    private const val PACKAGE = "cc.stadapp.stad"
    private const val FORMAT_VERSION: Byte = 1

    val message: NdefMessage
        get() = NdefMessage(
            // One version byte, so a later format can be told apart.
            NdefRecord.createMime(StadTagMime.TYPE, byteArrayOf(FORMAT_VERSION)),
            NdefRecord.createApplicationRecord(PACKAGE),
        )

    fun write(tag: Tag): WriteOutcome {
        val msg = message
        Ndef.get(tag)?.let { ndef ->
            return try {
                ndef.connect()
                when {
                    !ndef.isWritable -> WriteOutcome.CantWrite
                    ndef.maxSize < msg.byteArrayLength -> WriteOutcome.CantWrite
                    else -> {
                        ndef.writeNdefMessage(msg)
                        WriteOutcome.Written
                    }
                }
            } catch (_: IOException) {
                WriteOutcome.Failed
            } catch (_: FormatException) {
                WriteOutcome.Failed
            } catch (_: SecurityException) {
                // Tag out of date (moved away and back).
                WriteOutcome.Failed
            } finally {
                closeQuietly { ndef.close() }
            }
        }
        NdefFormatable.get(tag)?.let { formatable ->
            return try {
                formatable.connect()
                formatable.format(msg)
                WriteOutcome.Written
            } catch (_: IOException) {
                WriteOutcome.Failed
            } catch (_: FormatException) {
                WriteOutcome.CantWrite
            } catch (_: SecurityException) {
                WriteOutcome.Failed
            } finally {
                closeQuietly { formatable.close() }
            }
        }
        return WriteOutcome.CantWrite
    }

    /**
     * True if the tag's record reads back intact. If not, rewrites it once and reads it back again,
     * so a rewrite cut short by the tag moving can't be reported as success.
     */
    fun verifyOrRewrite(tag: Tag): Boolean =
        hasStadRecord(tag) || (write(tag) == WriteOutcome.Written && hasStadRecord(tag))

    private fun hasStadRecord(tag: Tag): Boolean {
        val ndef = Ndef.get(tag) ?: return false
        return try {
            ndef.connect()
            val records = ndef.ndefMessage?.records ?: return false
            val expected = message.records
            records.size >= 2 && records[0] == expected[0] && expected[1] in records
        } catch (_: IOException) {
            false
        } catch (_: FormatException) {
            false
        } catch (_: SecurityException) {
            false
        } finally {
            closeQuietly { ndef.close() }
        }
    }

    private inline fun closeQuietly(block: () -> Unit) {
        try {
            block()
        } catch (_: IOException) {
            // Already gone.
        } catch (_: SecurityException) {
            // Already gone.
        }
    }
}
