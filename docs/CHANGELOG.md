# Changelog

Per-slice detail. The index is the Current Status table in `CLAUDE.md`.

## Slice 1.2 — Blocking engine

- While locked, opening a blocked app sends you home and shows Stad's block screen ("X is blocked by Stad. Tap your tag to unlock." + **Go to home screen**; debug builds point to the Unlock (debug) button). While unlocked nothing is blocked. A debug-only **Lock now / Unlock** button on Home stands in for NFC until 1.3.
- `core/`: `BlockDecision` (locked + listed + not protected → block; never-confirmed protection → block nothing), `RelaunchGuard` (one block screen per burst of window events), `AccessibilityStatus` (parses the enabled-services setting, short and full forms), `LockState.fromStored`. Protected floor adds the system phone app and known emergency/safety apps.
- `data/`: `LockStateRepository` on the shared DataStore (`lock_state`, fails open to Unlocked). Block-list edits read the lock inside the same write and refuse while locked; the 1.1 in-memory lock is gone.
- `service/`: `BlockerAccessibilityService` (window-state events, activity windows only; re-reads protected apps for every candidate and doesn't block while unsure; goes Home first, retries if the block didn't take (fast 5 times, then every 10 s for as long as the app stays in front); skips a pending block screen if the user has moved to another app; re-checks the last app it saw when the lock turns on or the list changes; last package in memory only, never logged). `BlockedActivity` (not exported, no recents, own task, closes itself when it leaves the screen). `ProtectedPackages` adds every enabled keyboard.
- Home: blocking-service card (kept current while Stad is visible) with a Play-compliant disclosure (Agree / No thanks) before Accessibility settings, a hint for the greyed-out switch on sideloaded installs, a fallback if the phone has no Accessibility settings screen, and warnings when protected apps couldn't be confirmed or a lock change couldn't be saved.
- **Unit tests:** 65 (+30: `BlockDecisionTest` 8, `RelaunchGuardTest` 6, `RetryScheduleTest` 3, `AccessibilityStatusTest` 5, `LockStateTest` +2, `BlockListRulesTest` +1, `LockStateRepositoryTest` 3, `DataStoreBlockListRepositoryTest` +2).

## Slice 1.1 — Block list (PR #2)

- App picker: searchable (case- and accent-insensitive) list of launchable apps with icons and tick boxes; ticks save immediately. Home shows "N apps blocked" and **Choose apps**.
- `core/`: `AppEntry`, `BlockList`, `PickerRow`, `BlockListRules` (`setBlocked`, `sanitize`, `shouldPersistSanitized`, `effective`, `blockedCount`, `pickerRows`, `ALWAYS_PROTECTED`). The list is read-only while locked (switches on in 1.2).
- `data/`: `BlockListRepository` with an atomic `update`, backed by one app-wide Preferences DataStore; corrupt file reads as empty.
- `service/`: `InstalledApps`, `ProtectedPackages` (Stad, system UI, every home app, dialer, SMS app, Settings), `AppIcons` (cached 48dp bitmaps).
- Protection is applied on read, never written to storage; uninstalled apps are cleaned up only while unlocked and never from an empty app query.
- Manifest `<queries>` for LAUNCHER, HOME, DIAL, SENDTO, SETTINGS (no `QUERY_ALL_PACKAGES`); `dataExtractionRules` keeps settings off cloud backup and device transfer.
- CodeRabbit fixes: the picker stays read-only until every protected-app lookup has succeeded; a failed save keeps the last saved list and shows a warning; a storage read error shows an empty list and keeps retrying with back-off; a newer refresh cancels an older one.
- **Unit tests:** 35 (+30: `BlockListRulesTest` 20, `BlockListTest` 3, `DataStoreBlockListRepositoryTest` 7).

## Phase 0 — Foundation (PR #1, merged 2026-11-17)

- Repository set up: GPL-3.0 license, README, `.gitignore`, PR template.
- Docs: `01_START_HERE`, `02_ARCHITECTURE`, `03_PRODUCT_DESIGN`, `04_ROADMAP`, `05_GITHUB_WORKFLOW_GUIDE`, `06_PLAY_POLICY`, this changelog, and `CLAUDE.md`.
- App skeleton: Kotlin + Jetpack Compose + Material 3, package `cc.stadapp.stad`, min SDK 26, target/compile SDK 36, Gradle 8.14.3 wrapper, version catalog.
- `core/LockState` (`Unlocked`/`Locked`, `isLocked`, `toggled()`) — the domain type slices 1.2 and 1.3 build on.
- Placeholder home screen showing name, tagline and lock state; placeholder stop-sign launcher icon.
- CI: `android-ci.yml` (assembleDebug, testDebugUnitTest, lintDebug; uploads reports and the debug APK). Claude review (`claude-code-review.yml`) and `@claude` (`claude.yml`) workflows copied from Unnamed-RPG. CodeRabbit configured as the main PR reviewer (`.coderabbit.yaml`).
- **Unit tests:** 5 (`LockStateTest`).
