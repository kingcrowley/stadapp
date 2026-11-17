# Phase 0 — Foundation — Implementation Plan

**Spec:** `docs/superpowers/specs/2026-11-17-phase0-foundation-design.md`

## Task 1: Repository

- [x] Initial commit on `main`: GPL-3.0 `LICENSE`, README stub
- [x] Branch `phase/0-foundation`
- [x] `.gitignore` (Gradle, IDE, signing material)

## Task 2: Build skeleton

- [x] Gradle 8.14.3 wrapper
- [x] `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`
- [x] `gradle/libs.versions.toml` (AGP, Kotlin, Compose BOM, AndroidX, JUnit)
- [x] `app/build.gradle.kts`: namespace/applicationId `cc.stadapp.stad`, min 26, target/compile 36, Compose on, JDK 17 toolchain

## Task 3: App

- [x] `AndroidManifest.xml` with launcher `MainActivity`, `allowBackup=false`
- [x] `MainActivity` → `StadTheme { HomeScreen() }`
- [x] `ui/theme/Theme.kt` (dynamic colour on Android 12+, green fallback)
- [x] `ui/HomeScreen.kt` placeholder (name, tagline, lock state)
- [x] Placeholder vector launcher icon, strings, window theme

## Task 4: Domain + tests

- [x] `core/LockState.kt` (`Unlocked`/`Locked`, `isLocked`, `toggled()`)
- [x] `LockStateTest` (5 tests)

## Task 5: CI and review

- [x] `.github/workflows/android-ci.yml`: Temurin 17, setup-gradle, `assembleDebug testDebugUnitTest lintDebug`, upload reports + debug APK
- [x] `claude-code-review.yml` and `claude.yml` from Unnamed-RPG
- [x] PR template
- [x] CI green on PR #1
- [x] CodeRabbit set up as the PR reviewer (`.coderabbit.yaml`); Claude review kept, needs `CLAUDE_CODE_OAUTH_TOKEN`

## Task 6: Docs

- [x] `docs/01`–`06`, `CHANGELOG.md`, `CLAUDE.md`
- [x] README expanded

## Task 7: Manual check (David)

- [ ] Install `stad-debug-apk` from the CI run and confirm the home screen shows "Unlocked"
