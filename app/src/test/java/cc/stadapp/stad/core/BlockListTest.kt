package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockListTest {

    @Test
    fun emptyByDefault() {
        assertTrue(BlockList().isEmpty())
        assertEquals(0, BlockList().size)
    }

    @Test
    fun toggledAddsThenRemoves() {
        val once = BlockList().toggled("com.example.a")
        assertTrue("com.example.a" in once)
        assertFalse("com.example.a" in once.toggled("com.example.a"))
    }

    @Test
    fun withIsIdempotent() {
        val list = BlockList().with("a", true).with("a", true)
        assertEquals(1, list.size)
        assertTrue(list.with("a", false).isEmpty())
    }
}
