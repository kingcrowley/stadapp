package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockDecisionTest {

    private val insta = "com.instagram.android"
    private val list = BlockList(setOf(insta, "com.google.android.inputmethod.latin", STAD_PACKAGE))
    private val protected = setOf("com.android.launcher3", "com.google.android.inputmethod.latin")

    private fun decide(
        pkg: String?,
        lock: LockState = LockState.Locked,
        prot: Set<String>? = protected,
    ) = BlockDecision.decide(pkg, lock, list, prot)

    @Test
    fun unlockedAllowsEverything() {
        assertEquals(Decision.Allow, decide(insta, lock = LockState.Unlocked))
    }

    @Test
    fun lockedBlocksListedApp() {
        assertEquals(Decision.Block(insta), decide(insta))
    }

    @Test
    fun lockedAllowsUnlistedApp() {
        assertEquals(Decision.Allow, decide("com.example.maps"))
    }

    @Test
    fun protectedAppIsNeverBlocked() {
        assertEquals(Decision.Allow, decide("com.android.launcher3"))
    }

    @Test
    fun keyboardIsNeverBlockedEvenIfListed() {
        assertEquals(Decision.Allow, decide("com.google.android.inputmethod.latin"))
    }

    @Test
    fun stadIsNeverBlockedEvenIfListedOrMissingFromProtection() {
        assertEquals(Decision.Allow, decide(STAD_PACKAGE, prot = emptySet()))
    }

    @Test
    fun blankOrMissingPackageIsAllowed() {
        assertEquals(Decision.Allow, decide(""))
        assertEquals(Decision.Allow, decide("  "))
        assertEquals(Decision.Allow, decide(null))
    }

    @Test
    fun neverConfirmedProtectionFailsOpen() {
        assertEquals(Decision.Allow, decide(insta, prot = null))
    }
}
