# Stad — Architecture

## Stack

| Concern | Choice |
|---------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Min / target SDK | 26 (Android 8.0) / 36 |
| Build | Gradle (Kotlin DSL) + version catalog `gradle/libs.versions.toml` |
| Tests | JUnit 4 on the JVM (`app/src/test`); instrumented tests added when a slice needs them |
| Persistence | Jetpack Preferences DataStore, one app-wide instance (`Context.stadSettings`) |

## Layers

```
ui/        Compose screens, theme          ← depends on core, data
service/   AccessibilityService, NFC, boot  ← depends on core, data
data/      DataStore repositories           ← depends on core
core/      Pure Kotlin domain logic         ← depends on nothing Android
```

**Rule:** decisions live in `core/` as pure functions or small classes so they can be unit-tested on the JVM. Android components in `service/` and `ui/` stay thin: gather facts, call `core`, act on the result. (Same idea as the "pure resolver" pattern in the Unnamed-RPG project.)

## Data model

Small enough to live here rather than in its own document.

| Item | Type | Slice | Notes |
|------|------|-------|-------|
| Block list | `BlockList` (`Set<String>` of package names), DataStore key `blocked_packages` | 1.1 | Launchable apps only. Protected apps (Stad, system UI, every home app, dialer, SMS, Settings) can't be added, and are ignored on read if they become protected later. Uninstalled apps are cleaned up only while unlocked |
| Paired tag | `String` (hex tag UID) | 1.3 | Only this tag toggles the lock. Re-pairing requires being unlocked |
| Lock state | `LockState` (`Unlocked` / `Locked`) | 0 / 1.2 | Stored in the shared DataStore (`lock_state`) from 1.2, read by the app and the service; missing, unknown or unreadable values read as Unlocked. Reboot/force-stop guarantees are 2.1 |
| Block decision | `BlockDecision`, `RelaunchGuard`, `AccessibilityStatus` | 1.2 | Pure: block only when locked, listed and not protected; no protection ever confirmed → block nothing (fail open); one launch per burst of window events; parse the enabled-services setting |
| Sneaky look | last-used timestamp, peek length, cooldown | 2.3 | Times in minutes, configurable only while unlocked |

## Android components (planned)

| Component | Slice | Role |
|-----------|-------|------|
| `MainActivity` | 0 | Compose host; Home ↔ app picker; refreshes the app list on every `onStart` |
| `InstalledApps`, `ProtectedPackages`, `AppIcons` | 1.1 | Read launchable apps, the never-blockable set, and cached icons from `PackageManager` |
| `BlockListViewModel` | 1.1 | Combines stored list, apps, protected set, lock state and search into UI state via `BlockListRules` |
| `LockStateRepository` | 1.2 | Lock state on the shared DataStore. Block-list edits read it inside the same write and refuse while locked |
| `BlockerAccessibilityService` | 1.2 | Watches `TYPE_WINDOW_STATE_CHANGED` only, for activity windows only (not dialogs or overlays). For a locked, listed app it re-reads protected apps (and doesn't block while unsure), goes Home, then starts the block screen. Re-checks the last app it saw when the lock turns on or the list changes. Keeps the last package in memory only |
| `BlockedActivity` | 1.2 | Full-screen "X is blocked by Stad. Tap your tag to unlock." screen. Not exported, excluded from recents, own task; closes itself when it leaves the screen |
| NFC handling | 1.3 | Pairing writes a Stad NDEF record (Stad URI plus an Android Application Record) to the tag, and an `NDEF_DISCOVERED` filter for that record lets a tap reach Stad from anywhere. `TAG_DISCOVERED` is only a last-resort intent, so Stad can't rely on it. Another app with a matching filter can still win dispatch or show a chooser. Foreground dispatch while Stad is open always works, and opening Stad before tapping is the documented fallback. Supported tags: NDEF-formattable NFC Forum Type 2 (e.g. NTAG213/215/216). The 1.3 spec settles the details. |
| `BootReceiver` | 2.1 | Re-asserts the lock after reboot |
| Device admin / settings guards | 2.2 | Block uninstall and disabling the Accessibility service while locked |

## Permissions (planned)

| Permission | Why | Slice |
|-----------|-----|-------|
| `BIND_ACCESSIBILITY_SERVICE` (service) | Detect when a blocked app opens | 1.2 |
| No permission: `<queries>` for LAUNCHER, HOME, DIAL, SENDTO, SETTINGS | List launchable apps and find the never-blockable ones. `QUERY_ALL_PACKAGES` is not used | 1.1 |
| `NFC` | Read the tag UID | 1.3 |
| `RECEIVE_BOOT_COMPLETED` | Restore lock after reboot | 2.1 |

No `INTERNET` permission.
