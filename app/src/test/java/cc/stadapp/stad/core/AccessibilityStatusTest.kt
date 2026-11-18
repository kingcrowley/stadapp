package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityStatusTest {

    private val cls = "cc.stadapp.stad.service.BlockerAccessibilityService"

    private fun enabled(setting: String?) = AccessibilityStatus.isEnabled(setting, STAD_PACKAGE, cls)

    @Test
    fun fullFormMatches() {
        assertTrue(enabled("$STAD_PACKAGE/$cls"))
    }

    @Test
    fun shortFormMatches() {
        assertTrue(enabled("$STAD_PACKAGE/.service.BlockerAccessibilityService"))
    }

    @Test
    fun matchesAmongOtherServices() {
        assertTrue(enabled("com.other/.Svc:$STAD_PACKAGE/$cls:com.third/com.third.A"))
    }

    @Test
    fun otherServicesOnlyDoNotMatch() {
        assertFalse(enabled("com.other/.Svc:com.third/com.third.A"))
        assertFalse(enabled("$STAD_PACKAGE/.service.SomethingElse"))
        assertFalse(enabled("com.other/$cls"))
    }

    @Test
    fun emptyOrMissingSettingIsOff() {
        assertFalse(enabled(null))
        assertFalse(enabled(""))
    }
}
