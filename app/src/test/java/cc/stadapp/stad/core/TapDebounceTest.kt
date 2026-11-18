package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapDebounceTest {

    private val debounce = TapDebounce(windowMs = 1_500)

    @Test
    fun sameTagWithinWindowIsIgnored() {
        assertTrue(debounce.accept("04A1", 1_000))
        assertFalse(debounce.accept("04A1", 2_000))
    }

    @Test
    fun sameTagAfterWindowCounts() {
        assertTrue(debounce.accept("04A1", 1_000))
        assertTrue(debounce.accept("04A1", 2_500))
    }

    @Test
    fun differentTagCountsAtOnce() {
        assertTrue(debounce.accept("04A1", 1_000))
        assertTrue(debounce.accept("0499", 1_100))
    }

    @Test
    fun clockGoingBackwardsCounts() {
        assertTrue(debounce.accept("04A1", 5_000))
        assertTrue(debounce.accept("04A1", 1_000))
    }
}
