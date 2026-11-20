package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StadTagMimeTest {

    @Test
    fun matchesItsOwnType() {
        assertTrue(StadTagMime.matches(StadTagMime.TYPE))
    }

    @Test
    fun caseIsIgnored() {
        assertTrue(StadTagMime.matches("Application/VND.cc.StadApp.Stad"))
    }

    @Test
    fun anythingElseDoesNotMatch() {
        listOf(
            null, "", "application/vnd.cc.stadapp", "application/vnd.cc.stadapp.stad2",
            "application/vnd.cc.stadapp.stad; x=1", "text/plain", "https://stadapp.cc/t",
        ).forEach { assertFalse(it.toString(), StadTagMime.matches(it)) }
    }
}
