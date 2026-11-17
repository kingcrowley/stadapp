# Stad — Roadmap

## Philosophy

Build vertical slices. Every slice ends with something you can install on a phone and try, plus JVM unit tests for its core logic. Don't build systems in isolation.

## Phase 0: Foundation — in review (PR #1)

**Goal:** an empty, buildable app with docs and CI.

- [x] Repo, GPL-3.0 license, README
- [x] Docs set (this folder) + CLAUDE.md + changelog
- [x] Kotlin + Jetpack Compose skeleton (`cc.stadapp.stad`, min SDK 26, target 36)
- [x] `LockState` domain type with unit tests
- [x] CI: build + unit tests + lint on every PR, debug APK artifact
- [x] Claude PR review + `@claude` workflows

**Milestone:** the debug APK from CI installs on your phone and shows the Stad home screen; the first PR goes green and gets a Claude review.

## Phase 1: Core loop

### 1.1 — Block list
Pick apps from the installed launchable apps; the selection persists (DataStore).
**Test:** pick apps, kill and reopen Stad, the selection is still there.

### 1.2 — Blocking engine
AccessibilityService + block screen. A temporary debug-only "Lock now / Unlock" toggle drives `LockState` so blocking is testable **before tags arrive**.
**Test:** lock with the debug toggle, open a blocked app, see the block screen; unlock, it opens normally.

### 1.3 — NFC lock and unlock
Pair a tag by UID; tapping the paired tag toggles `LockState`; other tags are ignored. Debug toggle removed from release builds.
**Milestone:** tap the tag → Instagram is blocked; tap again → it opens.

## Phase 2: Strict mode

### 2.1 — Lock survives
Reboot and app update leave the lock in place. The lock is persisted, so it resumes after a force-stop as soon as Stad runs again (Android doesn't restart a force-stopped app on its own).

### 2.2 — Close the escape routes
While locked: block uninstalling Stad, disabling its Accessibility service, and reaching its App info screen (where Force stop lives).

### 2.3 — Sneaky look
Timed peek (default 5 min) with a cooldown (default 3 h), both configurable while unlocked.

## Phase 3: Release

### 3.1 — Onboarding and policy
Permission onboarding, Play's prominent disclosure for the Accessibility API, privacy policy page on stadapp.cc, real launcher icon.

### 3.2 — Ship it
Play Console listing ("Stad: NFC App Blocker"), Accessibility declaration, mandatory closed test (12 testers for 14 days on a new personal account), production release, F-Droid submission.

## Later ideas (unscheduled)

- Schedules (auto-lock at bedtime)
- Multiple tags / profiles (work vs home)
- QR code as an alternative to NFC
- Home-screen widget showing lock state
