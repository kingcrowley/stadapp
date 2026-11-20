package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCheckTest {

    @Test
    fun fourByteUidStarting08IsRandom() {
        assertTrue(PairingCheck.isRandomUid(byteArrayOf(0x08, 1, 2, 3)))
    }

    @Test
    fun ntagSevenByteUidIsNotRandom() {
        assertFalse(PairingCheck.isRandomUid(byteArrayOf(0x04, 1, 2, 3, 4, 5, 6)))
        assertFalse(PairingCheck.isRandomUid(byteArrayOf(0x04, 1, 2, 3)))
        assertFalse(PairingCheck.isRandomUid(byteArrayOf(0x08, 1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun sameIdTwiceConfirms() {
        assertEquals(PairingCheck.Result.Confirmed("04A1"), PairingCheck.confirm("04a1", "04:A1"))
    }

    @Test
    fun differentIdsAreUnstable() {
        assertEquals(PairingCheck.Result.Unstable, PairingCheck.confirm("04A1", "04A2"))
    }

    @Test
    fun missingIdIsMissing() {
        assertEquals(PairingCheck.Result.Missing, PairingCheck.confirm(null, "04A1"))
        assertEquals(PairingCheck.Result.Missing, PairingCheck.confirm("04A1", ""))
    }
}
