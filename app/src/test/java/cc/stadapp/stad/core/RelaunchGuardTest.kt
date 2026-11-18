package cc.stadapp.stad.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelaunchGuardTest {

    private val app = "com.instagram.android"

    @Test
    fun firstBlockLaunches() {
        assertTrue(RelaunchGuard().shouldLaunch(app, nowMs = 1_000))
    }

    @Test
    fun sameAppWithinWindowIsSkipped() {
        val guard = RelaunchGuard(windowMs = 500)
        guard.shouldLaunch(app, 1_000)
        assertFalse(guard.shouldLaunch(app, 1_300))
    }

    @Test
    fun sameAppAfterWindowLaunchesAgain() {
        val guard = RelaunchGuard(windowMs = 500)
        guard.shouldLaunch(app, 1_000)
        assertTrue(guard.shouldLaunch(app, 1_500))
    }

    @Test
    fun anotherAllowedAppInBetweenResetsTheGuard() {
        val guard = RelaunchGuard(windowMs = 500)
        guard.shouldLaunch(app, 1_000)
        guard.onAllowed("com.android.launcher3")
        assertTrue(guard.shouldLaunch(app, 1_100))
    }

    @Test
    fun stadsOwnBlockScreenDoesNotResetTheGuard() {
        val guard = RelaunchGuard(windowMs = 500)
        guard.shouldLaunch(app, 1_000)
        guard.onAllowed(STAD_PACKAGE)
        assertFalse(guard.shouldLaunch(app, 1_100))
    }

    @Test
    fun differentBlockedAppLaunchesAtOnce() {
        val guard = RelaunchGuard(windowMs = 500)
        guard.shouldLaunch(app, 1_000)
        assertTrue(guard.shouldLaunch("com.zhiliaoapp.musically", 1_050))
    }
}
