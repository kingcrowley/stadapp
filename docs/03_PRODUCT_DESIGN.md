# Stad — Product Design

## Core loop

1. **Set up (unlocked):** pick apps to block, pair a tag, grant the Accessibility permission.
2. **Lock:** tap the tag. A short confirmation ("Locked · 6 apps blocked") and a vibration.
3. **Locked:** opening a blocked app shows the block screen instead. Everything else works.
4. **Unlock:** tap the same tag. Confirmation + vibration.

## Screens

| Screen | Slice | Contents |
|--------|-------|----------|
| Home | 0 → 1.3 | Lock state, number of blocked apps, entry points to the block list and tag pairing |
| App picker | 1.1 | Searchable list of launchable apps with checkboxes. Disabled while locked |
| Block screen | 1.2 | "Blocked by Stad. Tap your tag to unlock." Button back to home screen. Sneaky-look button in 2.3 |
| Pair tag | 1.3 | "Hold your tag to the back of the phone." Shows success and lets you re-pair (unlocked only) |
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
| Force-stop Stad | Accessibility service is restarted by the system; state persisted | 2.1 |
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
