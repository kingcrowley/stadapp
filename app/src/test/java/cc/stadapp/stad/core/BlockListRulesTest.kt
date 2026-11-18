package cc.stadapp.stad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockListRulesTest {

    private val protectedPkgs = setOf("cc.stadapp.stad", "com.android.launcher", "com.android.dialer")
    private val unlocked = LockState.Unlocked
    private val locked = LockState.Locked

    @Test
    fun canEditOnlyWhenUnlocked() {
        assertTrue(BlockListRules.canEdit(unlocked))
        assertFalse(BlockListRules.canEdit(locked))
    }

    @Test
    fun cannotEditUntilProtectionIsConfirmed() {
        assertFalse(BlockListRules.canEdit(unlocked, protectionComplete = false))
        val result = BlockListRules.setBlocked(BlockList(), "x", true, unlocked, protectedPkgs, protectionComplete = false)
        assertTrue(result.isEmpty())
        val rows = BlockListRules.pickerRows(apps, BlockList(), protectedPkgs, unlocked, protectionComplete = false)
        assertTrue(rows.none { it.enabled })
    }

    @Test
    fun protectedAndBlankPackagesAreNotBlockable() {
        assertFalse(BlockListRules.isBlockable("com.android.dialer", protectedPkgs))
        assertFalse(BlockListRules.isBlockable("", protectedPkgs))
        assertTrue(BlockListRules.isBlockable("com.instagram.android", protectedPkgs))
    }

    @Test
    fun setBlockedAddsAndRemovesWhenUnlocked() {
        val on = BlockListRules.setBlocked(BlockList(), "x", true, unlocked, protectedPkgs)
        assertTrue("x" in on)
        val off = BlockListRules.setBlocked(on, "x", false, unlocked, protectedPkgs)
        assertFalse("x" in off)
    }

    @Test
    fun setBlockedIsNoOpWhenLocked() {
        val list = BlockList(setOf("x"))
        assertSame(list.packages, BlockListRules.setBlocked(list, "y", true, locked, protectedPkgs).packages)
        assertSame(list.packages, BlockListRules.setBlocked(list, "x", false, locked, protectedPkgs).packages)
    }

    @Test
    fun cannotBlockProtectedPackage() {
        val result = BlockListRules.setBlocked(BlockList(), "com.android.dialer", true, unlocked, protectedPkgs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun canUnblockPackageThatBecameProtected() {
        val list = BlockList(setOf("com.android.launcher"))
        val result = BlockListRules.setBlocked(list, "com.android.launcher", false, unlocked, protectedPkgs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun sanitizeDropsOnlyUninstalled() {
        val saved = BlockList(setOf("kept", "uninstalled", "com.android.launcher"))
        val installed = setOf("kept", "com.android.launcher")
        val result = BlockListRules.sanitize(saved) { it in installed }
        assertEquals(setOf("kept", "com.android.launcher"), result.packages)
    }

    @Test
    fun sanitizeKeepsCleanList() {
        val saved = BlockList(setOf("a", "b"))
        assertEquals(saved, BlockListRules.sanitize(saved) { true })
    }

    @Test
    fun persistSanitizedOnlyWhenUnlockedNonEmptyAndChanged() {
        val saved = BlockList(setOf("a", "gone"))
        val sanitized = BlockList(setOf("a"))
        assertTrue(BlockListRules.shouldPersistSanitized(saved, sanitized, unlocked, installedAppCount = 40))
        assertFalse(BlockListRules.shouldPersistSanitized(saved, sanitized, locked, installedAppCount = 40))
        assertFalse(BlockListRules.shouldPersistSanitized(saved, sanitized, unlocked, installedAppCount = 0))
        assertFalse(BlockListRules.shouldPersistSanitized(saved, saved, unlocked, installedAppCount = 40))
    }

    @Test
    fun protectedPackageStaysStoredButIsNotEffective() {
        val stored = BlockList(setOf("com.instagram.android", "com.android.launcher"))
        assertEquals(setOf("com.instagram.android"), BlockListRules.effective(stored, protectedPkgs).packages)
        assertEquals(1, BlockListRules.blockedCount(stored, protectedPkgs))
        assertEquals(stored, BlockListRules.sanitize(stored) { true })
    }

    @Test
    fun alwaysProtectedCoversStadAndSystem() {
        val floor = BlockListRules.ALWAYS_PROTECTED
        assertTrue(STAD_PACKAGE in floor)
        assertTrue("com.android.systemui" in floor)
        assertTrue("android" in floor)
    }

    private val apps = listOf(
        AppEntry("com.instagram.android", "Instagram"),
        AppEntry("com.zhiliaoapp.musically", "TikTok"),
        AppEntry("com.example.cafe", "Café Finder"),
        AppEntry("com.android.dialer", "Phone"),
        AppEntry("com.b.mail", "Mail"),
        AppEntry("com.a.mail", "Mail"),
        AppEntry("com.example.nolabel", ""),
    )

    private fun rows(query: String = "", list: BlockList = BlockList(), lock: LockState = unlocked) =
        BlockListRules.pickerRows(apps, list, protectedPkgs, lock, query)

    @Test
    fun rowsSortedByNameWithPackageTieBreak() {
        assertEquals(
            listOf("Café Finder", "com.example.nolabel", "Instagram", "Mail", "Mail", "Phone", "TikTok"),
            rows().map { it.app.displayName },
        )
        val mails = rows("mail").map { it.app.packageName }
        assertEquals(listOf("com.a.mail", "com.b.mail"), mails)
    }

    @Test
    fun searchIsCaseAndAccentInsensitive() {
        assertEquals(listOf("com.example.cafe"), rows("CAFE").map { it.app.packageName })
        assertEquals(listOf("com.example.cafe"), rows("café").map { it.app.packageName })
    }

    @Test
    fun searchMatchesPackageName() {
        assertEquals(listOf("com.zhiliaoapp.musically"), rows("musically").map { it.app.packageName })
    }

    @Test
    fun blankQueryShowsEverything() {
        assertEquals(apps.size, rows("   ").size)
    }

    @Test
    fun protectedRowsAreDisabledAndUnchecked() {
        val phone = rows(list = BlockList(setOf("com.android.dialer"))).single { it.app.packageName == "com.android.dialer" }
        assertTrue(phone.protected)
        assertFalse(phone.enabled)
        assertFalse(phone.checked)
    }

    @Test
    fun checkedReflectsList() {
        val insta = rows(list = BlockList(setOf("com.instagram.android"))).single { it.app.label == "Instagram" }
        assertTrue(insta.checked)
        assertTrue(insta.enabled)
    }

    @Test
    fun allRowsDisabledWhenLocked() {
        assertTrue(rows(lock = locked).none { it.enabled })
    }

    @Test
    fun duplicatePackagesShownOnce() {
        val dupes = apps + AppEntry("com.instagram.android", "Instagram")
        assertEquals(apps.size, BlockListRules.pickerRows(dupes, BlockList(), protectedPkgs, unlocked).size)
    }
}
