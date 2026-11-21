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
| Lock state | `LockState` (`Unlocked` / `Locked`) | 0 / 1.2 | Stored in the shared DataStore (`lock_state`) from 1.2, read by the app and the service; missing, unknown or unreadable values read as Unlocked. From 2.1 the service goes Home once on connect while locked if it can't yet tell which app is in front (reboot, update, re-enable), and Home and a boot notification warn when locked but the service is off |
| Block decision | `BlockDecision`, `RelaunchGuard`, `AccessibilityStatus` | 1.2 | Pure: block only when locked, listed and not protected; no protection ever confirmed → block nothing (fail open); one launch per burst of window events; parse the enabled-services setting |
| Lock survival | `ConnectCheck`, `BlockingHealth`, `BootCheck` | 2.1 | Pure: on connect, go Home once while locked with apps listed and the foreground unknown (checked on every stored change, so a read error first reading as Unlocked still re-locks); `OffWhileLocked` when the service is off while locked; only `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` are handled |
| Notification prompt | `Boolean`, DataStore key `notify_prompt_dismissed` | 2.1 | "Not now" on Home's notification card |
| Sneaky look | last-used timestamp, peek length, cooldown | 2.3 | Times in minutes, configurable only while unlocked |

## Android components (planned)

| Component | Slice | Role |
|-----------|-------|------|
| `MainActivity` | 0 | Compose host; Home ↔ app picker; refreshes the app list on every `onStart`. From 1.3, reads tags in reader mode while resumed (pairing or lock/unlock) |
| `InstalledApps`, `ProtectedPackages`, `AppIcons` | 1.1 | Read launchable apps, the never-blockable set, and cached icons from `PackageManager` |
| `BlockListViewModel` | 1.1 | Combines stored list, apps, protected set, lock state and search into UI state via `BlockListRules` |
| `LockStateRepository` | 1.2 | Lock state on the shared DataStore. Block-list edits read it inside the same write and refuse while locked |
| `BlockerAccessibilityService` | 1.2 | Watches `TYPE_WINDOW_STATE_CHANGED` only (ignoring Stad's own `TagActivity`), for activity windows only (not dialogs or overlays). For a locked, listed app it re-reads protected apps (and doesn't block while unsure), goes Home, then starts the block screen. Re-checks the last app it saw when the lock turns on or the list changes. From 2.1, on connect it cancels the "blocking is off" notification and, while locked with the foreground app unknown, goes Home once (`ConnectCheck`). Keeps the last package in memory only |
| `BlockedActivity` | 1.2 | Full-screen "X is blocked by Stad. Tap your tag to unlock." screen. Not exported, excluded from recents, own task; closes itself when it leaves the screen or the lock turns off |
| `TagRepository` | 1.3 | Paired tag UID on the shared DataStore. `pair` refuses while locked; `tap` reads the paired tag and lock and writes the new lock in one atomic edit |
| NFC handling | 1.3 | Pairing (two taps) writes a Stad NDEF message (a MIME record `application/vnd.cc.stadapp.stad` first, then an Android Application Record; not a web link, because Android 16+ sends `http(s)` tags to `ACTION_VIEW`) and checks it reads back. Identity is the tag UID, not the record; tags with a random UID are refused. Tapped anywhere: `TagActivity` (translucent, own task, exact `NDEF_DISCOVERED` + `DEFAULT` filter on Stad's MIME type) toggles the lock, vibrates and shows a toast, then closes. Another app with a matching filter can still win dispatch or show a chooser, so opening Stad before tapping is the documented fallback: while open, `MainActivity` reads tags itself in reader mode (A, B, F, V). Taps of the same tag within 1.5 s are ignored. Supported tags: NFC Forum Type 2, e.g. NTAG213/215/216 |
| `BootReceiver` | 2.1 | Not exported. On `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`, reads the lock (5 s timeout, fails open) and the enabled-services setting; if locked but the service is off, posts the `BlockingWarning` notification (one fixed id, opens Home). Never starts or enables the service: Android binds an enabled one itself |
| Device admin / settings guards | 2.2 | Block uninstall and disabling the Accessibility service while locked |

## Permissions (planned)

| Permission | Why | Slice |
|-----------|-----|-------|
| `BIND_ACCESSIBILITY_SERVICE` (service) | Detect when a blocked app opens | 1.2 |
| No permission: `<queries>` for LAUNCHER, HOME, DIAL, SENDTO, SETTINGS | List launchable apps and find the never-blockable ones. `QUERY_ALL_PACKAGES` is not used | 1.1 |
| `NFC` | Read and write the tag | 1.3 |
| `VIBRATE` | Lock / unlock feedback on a tap | 1.3 |
| `uses-feature android.hardware.nfc` (required) | Stad is useless without NFC, so Play hides it from phones without it | 1.3 |
| `RECEIVE_BOOT_COMPLETED` | Check after a reboot that blocking is still on | 2.1 |
| `POST_NOTIFICATIONS` (Android 13+, runtime) | The "locked, but blocking is off" warning. Asked from a Home card (Allow / Not now); without it Home still warns | 2.1 |

No `INTERNET` permission.
