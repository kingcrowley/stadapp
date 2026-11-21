package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootCheckTest {

    @Test
    fun handlesBootAndUpdate() {
        assertTrue(BootCheck.handles("android.intent.action.BOOT_COMPLETED"))
        assertTrue(BootCheck.handles("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun ignoresEverythingElse() {
        listOf(
            null, "", "android.intent.action.LOCKED_BOOT_COMPLETED", "android.intent.action.PACKAGE_REPLACED",
            "android.intent.action.boot_completed",
        ).forEach { assertFalse(it.toString(), BootCheck.handles(it)) }
    }
}
