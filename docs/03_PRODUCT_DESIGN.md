# Stad — Product Design

## Core loop

1. **Set up (unlocked):** pick apps to block, pair a tag, grant the Accessibility permission.
2. **Lock:** tap the tag. A short confirmation ("Locked · 6 apps blocked") and a vibration.
3. **Locked:** opening a blocked app shows the block screen instead. Everything else works.
4. **Unlock:** tap the same tag. Confirmation + vibration.

## Screens

| Screen | Slice | Contents |
|--------|-------|----------|
| Home | 0 → 1.3 | Lock state, number of blocked apps, blocking-service card, tag card (pair / re-pair; while locked, explains the lost-tag route), NFC off / missing card, entry to the block list |
| App picker | 1.1 | Searchable list of launchable apps with checkboxes. Disabled while locked |
| Block screen | 1.2 | "X is blocked by Stad. Tap your tag to unlock." with the app's icon and **Go to home screen**; Back also goes home. Sneaky-look button in 2.3 |
| Pair tag | 1.3 | Dialog: "Hold your tag to the back of the phone." → "Tag written. Tap it once more to check it." → "Tag paired." Errors for a tag that can't be written, a random-ID tag, a moved tag and a different second tag. Re-pairing replaces the old tag (unlocked only) |
| Tap feedback | 1.3 | Tapping the paired tag anywhere: one buzz + "Locked" (with a warning if no apps are chosen or the blocking service is off), two buzzes + "Unlocked". Another tag: three buzzes + "That's not your Stad tag" |
| Settings | 2.3 | Sneaky look length and cooldown |
| Onboarding | 3.1 | Explains the Accessibility permission (Play's prominent disclosure), NFC check, first pairing |

## Rules

- **Nothing about blocking can be changed while locked.** The block list, the paired tag, and the sneaky-look settings are read-only until you unlock. Otherwise "unlock" is just "edit the list".
- **Only the paired tag unlocks.** Any other NFC tag is ignored (with a gentle "That's not your Stad tag" message).
- **Never blockable:** Stad itself, the launcher, the Phone/Dialer app, emergency services. A lock must never stop someone calling for help.
- **Lost tag:** there is no in-app bypass. The documented recovery path is turning off Stad's Accessibility service in system settings — which slice 2.2 makes harder while locked, so the final recovery is a safe-mode reboot. This trade-off is deliberate and will be stated plainly in onboarding.

## Strict mode (Phase 2)

| Escape route | Response | Slice |
|--------------|----------|-------|
| Reboot | Lock state persisted; service restarts; boot receiver re-checks | 2.1 |
| Force-stop Stad | Not automatically recoverable: since Android 15 a force-stopped app stays stopped until the user opens it, and the system may disable its Accessibility service. So while locked, Stad blocks its own App info screen so Force stop can't be reached. If a force-stop happens anyway (e.g. via adb), the saved lock state survives, but blocking is off until the user opens Stad and, if Android disabled it, re-enables Stad's Accessibility service in system settings. Stad detects this on open and walks the user through it. Onboarding says this plainly rather than promising more | 2.1 / 2.2 |
| App update | State persisted across `MY_PACKAGE_REPLACED` | 2.1 |
| Uninstall Stad | Device admin prevents uninstall while locked (exact mechanism decided in the 2.2 spec) | 2.2 |
| Disable Accessibility service | Block the Accessibility settings screen while locked | 2.2 |

## Sneaky look (slice 2.3)

- From the block screen, "Sneaky look" opens the blocked app for **N minutes** (default 5).
- Afterwards it re-locks automatically and the button is unavailable for **M hours** (default 3), shown as "Next sneaky look in 2h 14m".
- One peek covers all blocked apps (not per app).
- N and M can only be changed while unlocked.

## Accessibility (of Stad itself)

- All UI meets Material 3 contrast and 48dp touch targets; supports font scaling and TalkBack labels.
- Lock and unlock are confirmed by text **and** haptics, not colour alone.
- The block screen must be readable by TalkBack and offer a clear way back to the home screen.
