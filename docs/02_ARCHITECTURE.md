# Stad — Architecture

## Stack

| Concern | Choice |
|---------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Min / target SDK | 26 (Android 8.0) / 36 |
| Build | Gradle (Kotlin DSL) + version catalog `gradle/libs.versions.toml` |
| Tests | JUnit 4 on the JVM (`app/src/test`); instrumented tests added when a slice needs them |
| Persistence | Jetpack DataStore (planned, slice 1.1) |

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
| Block list | `Set<String>` of package names | 1.1 | Launchable apps only; Stad itself, the launcher, Phone and Settings can never be blocked |
| Paired tag | `String` (hex tag UID) | 1.3 | Only this tag toggles the lock. Re-pairing requires being unlocked |
| Lock state | `LockState` (`Unlocked` / `Locked`) | 0 / 1.2 | Persisted so it survives restarts (2.1) |
| Sneaky look | last-used timestamp, peek length, cooldown | 2.3 | Times in minutes, configurable only while unlocked |

## Android components (planned)

| Component | Slice | Role |
|-----------|-------|------|
| `MainActivity` | 0 | Compose host |
| `BlockerAccessibilityService` | 1.2 | Watches `TYPE_WINDOW_STATE_CHANGED`; if the foreground package is blocked and state is `Locked`, shows the block screen |
| `BlockedActivity` | 1.2 | Full-screen "Blocked, tap your tag to unlock" screen |
| NFC handling | 1.3 | Foreground dispatch while Stad is open, plus a `TAG_DISCOVERED` intent filter so a tap works from anywhere |
| `BootReceiver` | 2.1 | Re-asserts the lock after reboot |
| Device admin / settings guards | 2.2 | Block uninstall and disabling the Accessibility service while locked |

## Permissions (planned)

| Permission | Why | Slice |
|-----------|-----|-------|
| `BIND_ACCESSIBILITY_SERVICE` (service) | Detect when a blocked app opens | 1.2 |
| `QUERY_ALL_PACKAGES` or a `<queries>` launcher intent | List installed apps for the picker. Prefer `<queries>` (Play restricts `QUERY_ALL_PACKAGES`) | 1.1 |
| `NFC` | Read the tag UID | 1.3 |
| `RECEIVE_BOOT_COMPLETED` | Restore lock after reboot | 2.1 |

No `INTERNET` permission.
