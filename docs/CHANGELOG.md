# Changelog

Per-slice detail. The index is the Current Status table in `CLAUDE.md`.

## Phase 0 — Foundation (PR #1)

- Repository set up: GPL-3.0 license, README, `.gitignore`, PR template.
- Docs: `01_START_HERE`, `02_ARCHITECTURE`, `03_PRODUCT_DESIGN`, `04_ROADMAP`, `05_GITHUB_WORKFLOW_GUIDE`, `06_PLAY_POLICY`, this changelog, and `CLAUDE.md`.
- App skeleton: Kotlin + Jetpack Compose + Material 3, package `cc.stadapp.stad`, min SDK 26, target/compile SDK 36, Gradle 8.14.3 wrapper, version catalog.
- `core/LockState` (`Unlocked`/`Locked`, `isLocked`, `toggled()`) — the domain type slices 1.2 and 1.3 build on.
- Placeholder home screen showing name, tagline and lock state; placeholder stop-sign launcher icon.
- CI: `android-ci.yml` (assembleDebug, testDebugUnitTest, lintDebug; uploads reports and the debug APK). Claude review (`claude-code-review.yml`) and `@claude` (`claude.yml`) workflows copied from Unnamed-RPG.
- **Unit tests:** 5 (`LockStateTest`).
