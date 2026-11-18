package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StadTagUriTest {

    @Test
    fun matchesItsOwnValue() {
        assertTrue(StadTagUri.matches(StadTagUri.VALUE))
    }

    @Test
    fun schemeAndHostAreCaseInsensitive() {
        assertTrue(StadTagUri.matches("HTTPS://StadApp.CC/t"))
    }

    @Test
    fun anythingElseDoesNotMatch() {
        listOf(
            null, "", "https://stadapp.cc", "https://stadapp.cc/", "https://stadapp.cc/t/",
            "https://stadapp.cc/T", "https://stadapp.cc/t?x=1", "http://stadapp.cc/t",
            "https://evil.cc/t", "https://stadapp.cc.evil/t", "stadapp.cc/t",
        ).forEach { assertFalse(it.toString(), StadTagUri.matches(it)) }
    }
}
