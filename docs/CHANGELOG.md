# Changelog

Per-slice detail. The index is the Current Status table in `CLAUDE.md`.

## Slice 2.2 — Blocking history (PR #6, in review)

- Replaces the planned "close the escape routes": Google Play forbids an app blocking its own uninstall, its Accessibility switch or Force stop, so Stad stays easy to turn off and instead keeps an honest record. A strict F-Droid build that closes the routes is a later idea.
- While locked, Stad records each time blocking was off: the Accessibility service turned off, or Stad force-stopped. Home shows "While locked, blocking was off N times in the last 7 days" and up to 5 entries ("Today 14:02 – 14:20 (18 min)"; "stopped some time after …" when the start is only estimated). **Clear history** only while unlocked. On the phone only.
- A stop counts only if Android's enabled-services setting no longer lists Stad, so reboots, updates, crashes and user switches aren't recorded. Force-stop times come from Android's process exit records (Android 11+, force-stops only), which also catch a force-stop followed by turning blocking back on straight from Settings; below Android 11 the start is estimated from when blocking was last seen working, never before the lock. Toggles under 5 s are ignored.
- `core/`: `EscapeLog` (cap 50, last-7-days view, versioned encoding, corrupt reads as empty), `EscapeTracker` (pure transitions).
- `data/`: `EscapeRepository`; `TagRepository.tap` and `LockStateRepository.set` update the history in the same edit as the lock.
- `service/`: the blocking service records its stop synchronously in `onUnbind` (setting-checked), its connect, and "last seen working" at most every 15 min while locked; `BootReceiver`, Home and tag taps note "found off"; `ForceStops` reads exit records.
- The Accessibility disclosure gains: "If you turn it off while Stad is locked, Stad notes when, so you can see it later on this phone."
- Review fixes: only exit records that are force-stops count (not a Recents removal, or an update below Android 14: `ForceStopRecord`); the tag tap, debug lock and found-off checks read the service state inside their edit (`BlockingProbe`); day labels by calendar date; Home's clock refreshes each minute; the card and Clear stay while older entries are stored.
- **Unit tests:** 160 (+32: `EscapeTrackerTest` 16, `EscapeLogTest` 5, `ForceStopRecordTest` 4, `HistoryDaysTest` 3, `EscapeRepositoryTest` 4).

## Slice 2.1 — Lock survives (PR #5, in review)

- After a reboot, an update of Stad or the service reconnecting, a locked phone blocks again with no user action as long as Stad's Accessibility service is still on, including the app already on screen (if it's off, the lock survives but the user must turn it back on; see below): the service can't see what's in front until the next screen change, so while locked with apps listed and the foreground unknown it goes Home once (retrying every second if Android refuses). Cost: after an update while locked, the app that was open goes to the background once, even an unblocked one.
- When Stad is locked but its Accessibility service is off (a force-stop always turns it off), Home's service card turns into a red warning, "Stad is locked, but blocking is off", with **Turn on**. After a boot or update a notification says the same; tapping it opens Home. It's cancelled when the service connects and whenever Home is shown.
- Home asks for the notification permission on Android 13+ (Allow / Not now; "Not now" and a refusal are remembered). Without it Home still warns.
- `core/`: `ConnectCheck` (Home once on connect, checked on every stored change so a read error first reading as Unlocked still re-locks), `BlockingHealth` (`Ok` / `OffUnlocked` / `OffWhileLocked`), `BootCheck` (only `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`).
- `data/`: `NotifyPromptRepository` (`notify_prompt_dismissed`).
- `service/`: `BootReceiver` (not exported; `goAsync`, lock read with a 5 s timeout that fails open), `BlockingWarning` (channel "Blocking warnings", one fixed id; a check that started before a cancel can't post afterwards), `ServiceStatus` (shared enabled-service check, now also used by tag taps and Home).
- Manifest: `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, the receiver.
- Not prevented yet: force-stop and turning the service off while locked (2.2).
- **Unit tests:** 128 (+14: `ConnectCheckTest` 7, `BlockingHealthTest` 3, `BootCheckTest` 2, `NotifyPromptRepositoryTest` 2).

## Slice 1.3 — NFC lock and unlock (PR #4, in review)

- Pair a tag from Home (two taps: write, then check), then tap it anywhere to lock and tap again to unlock. Other tags do nothing but say "That's not your Stad tag". A buzz pattern and a toast confirm each tap. The block screen closes itself on unlock. The debug Lock / Unlock button stays, in debug builds only.
- `core/`: `TagId` (UID → uppercase hex), `TagAction.decide`, `StadTagMime` (the tag's record type; a MIME record, since Android 16+ sends web-link tags to the browser path), `PairingCheck` (refuses random UIDs; second tap must match), `PairingFlow` (pairing steps and errors, tag I/O passed in), `TagFeedback` (message + haptic per outcome), `TapDebounce` (same tag within 1.5 s ignored, per tag).
- `data/`: `TagRepository` (`paired_tag` key; `pair` refused while locked; `tap` decides and writes the lock in one edit, a failed write leaves the lock unchanged).
- `service/`: `TagActivity` (translucent, exported only for the Stad MIME-type filter), `TagTaps`, `TagWriter` (Stad MIME record then AAR; formats blank tags; checks writable and size), `Haptics`. The blocking service ignores `TagActivity`.
- Home: tag card (pair / re-pair, disabled while locked with the lost-tag explanation), NFC off / missing card with a way to NFC settings, pairing dialog. `MainActivity` reads tags in reader mode while open.
- Manifest: `NFC`, `VIBRATE`, NFC hardware required.
- **Unit tests:** 114 (+49: `TagIdTest` 4, `TagActionTest` 5, `StadTagMimeTest` 3, `PairingCheckTest` 5, `PairingFlowTest` 12, `TagFeedbackTest` 7, `TapDebounceTest` 5, `TagRepositoryTest` 8).

## Slice 1.2 — Blocking engine (PR #3, merged 2026-11-18)

- While locked, opening a blocked app sends you home and shows Stad's block screen ("X is blocked by Stad. Tap your tag to unlock." + **Go to home screen**; debug builds point to the Unlock (debug) button). While unlocked nothing is blocked. A debug-only **Lock now / Unlock** button on Home stands in for NFC until 1.3.
- `core/`: `BlockDecision` (locked + listed + not protected → block; never-confirmed protection → block nothing), `RelaunchGuard` (one block screen per burst of window events), `AccessibilityStatus` (parses the enabled-services setting, short and full forms), `LockState.fromStored`. Protected floor adds the system phone app and known emergency/safety apps.
- `data/`: `LockStateRepository` on the shared DataStore (`lock_state`, fails open to Unlocked). Block-list edits read the lock inside the same write and refuse while locked; the 1.1 in-memory lock is gone.
- `service/`: `BlockerAccessibilityService` (window-state events, activity windows only; re-reads protected apps for every candidate and doesn't block while unsure; goes Home first, retries if the block didn't take (fast 5 times, then every 10 s for as long as the app stays in front); skips a pending block screen if the user has moved to another app; re-checks the last app it saw when the lock turns on or the list changes; last package in memory only, never logged). `BlockedActivity` (not exported, no recents, own task, closes itself when it leaves the screen). `ProtectedPackages` adds every enabled keyboard.
- Home: blocking-service card (kept current while Stad is visible) with a Play-compliant disclosure (Agree / No thanks) before Accessibility settings, a hint for the greyed-out switch on sideloaded installs, a fallback if the phone has no Accessibility settings screen, and warnings when protected apps couldn't be confirmed or a lock change couldn't be saved.
- **Unit tests:** 65 (+30: `BlockDecisionTest` 8, `RelaunchGuardTest` 6, `RetryScheduleTest` 3, `AccessibilityStatusTest` 5, `LockStateTest` +2, `BlockListRulesTest` +1, `LockStateRepositoryTest` 3, `DataStoreBlockListRepositoryTest` +2).

## Slice 1.1 — Block list (PR #2, merged 2026-11-18)

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
