# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Stad** (Irish for "stop") is an open-source Android app blocker. The user picks distracting apps; tapping a paired NFC tag locks them, and tapping the same tag again unlocks them. Strict by design: no in-app "unlock" button, with a rate-limited "sneaky look" planned for Phase 2. Licensed GPL-3.0. Targets Google Play and F-Droid.

- Domain: https://stadapp.cc · Package / applicationId: `cc.stadapp.stad`
- Play listing name (planned): "Stad: NFC App Blocker"

## Build & Run

- **Tooling:** Android Studio (latest stable), JDK 17, Gradle wrapper (`./gradlew`), AGP + Kotlin versions in `gradle/libs.versions.toml`
- **Build debug APK:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`
- **Install on a connected phone:** `./gradlew installDebug`
- **Unit tests (JVM):** `./gradlew testDebugUnitTest`
- **Single test class:** `./gradlew testDebugUnitTest --tests "cc.stadapp.stad.core.LockStateTest"`
- **Lint:** `./gradlew lintDebug`
- **CI:** `.github/workflows/android-ci.yml` runs build + unit tests + lint on every PR and uploads the debug APK as an artifact.

## Architecture

See `docs/02_ARCHITECTURE.md`. In short:

1. **core/** — pure Kotlin domain logic (lock state, block list rules, sneaky-look timing). No Android imports, fully JVM-unit-tested. Put logic here whenever possible.
2. **data/** — persistence (block list, paired tag id, lock state) in one app-wide Preferences DataStore. Slice 1.1+.
3. **service/** — Android integration: AccessibilityService that detects blocked apps (1.2), NFC tag handling (1.3), boot receiver (2.1).
4. **ui/** — Jetpack Compose screens + theme.

## Key Design Decisions

- **Strict by default.** Only the paired tag unlocks. Any escape hatch (sneaky look, emergency) is rate-limited and designed in `docs/03_PRODUCT_DESIGN.md`, never ad hoc.
- **Blocking uses an AccessibilityService.** This is a Play-policy-sensitive permission: every slice touching it must keep `docs/06_PLAY_POLICY.md` true (prominent disclosure, no data collection, declared use).
- **Offline, no accounts, no analytics.** No network permission unless a later slice justifies it.
- **Testable slices.** Each slice ends with something you can install and try, plus JVM unit tests for its core logic.
- Flag deliberate shortcuts with `TODO(slice-id)` markers so they can be grepped.

## Workflow

Phases → slices. One branch per slice (`phase/N-sliceX-name`), a spec + plan in `docs/superpowers/specs/` and `docs/superpowers/plans/`, a PR reviewed by the Claude review workflow, squash-merge into `main`, tag `PhaseN-SliceX-Complete`. Full detail: `docs/05_GITHUB_WORKFLOW_GUIDE.md`.

## Current Status

Per-slice detail lives in `docs/CHANGELOG.md`; this table is the index.

| Phase / Slice | State | Unit tests | Tag / PR |
|---------------|-------|-----------|----------|
| Phase 0 — Foundation | Merged | 5 | PR #1, `Phase0-Foundation-Complete` |
| 1.1 — Block list | Merged | 35 | PR #2, `Phase1-Slice1-Complete` |
| 1.2 — Blocking engine | Merged | 65 | PR #3, `Phase1-Slice2-Complete` |
| 1.3 — NFC lock | In review | 113 | PR #4 |

**Next up:** Slice 1.3 — NFC pairing + lock/unlock, in review (PR #4). Spec: `docs/superpowers/specs/2026-11-18-slice1.3-nfc-lock-design.md`; plan: `docs/superpowers/plans/2026-11-18-slice1.3-nfc-lock.md`. Then Phase 2 (strict mode), starting with 2.1 — lock survives reboot.
