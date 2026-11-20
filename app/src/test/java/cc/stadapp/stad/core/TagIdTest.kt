package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagIdTest {

    @Test
    fun bytesBecomeUppercaseHex() {
        assertEquals("04A1FF00", TagId.fromBytes(byteArrayOf(0x04, 0xA1.toByte(), 0xFF.toByte(), 0x00)))
    }

    @Test
    fun emptyBytesGiveNull() {
        assertNull(TagId.fromBytes(byteArrayOf()))
    }

    @Test
    fun normalizeStripsSeparatorsAndUppercases() {
        assertEquals("04A1FF", TagId.normalize(" 04:a1-ff "))
    }

    @Test
    fun normalizeRejectsBlankAndNonHex() {
        assertNull(TagId.normalize(null))
        assertNull(TagId.normalize("  "))
        assertNull(TagId.normalize("04ZZ"))
    }
}
