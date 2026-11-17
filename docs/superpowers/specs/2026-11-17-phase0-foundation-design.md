# Phase 0 — Foundation — Design

**Date:** 2026-11-17
**Branch:** `phase/0-foundation`
**Status:** in review (PR #1)

## Problem

There is no project yet. Before any feature work, Stad needs a repository that builds, tests and reviews itself, and a written plan that later slices follow, so each slice can be small and testable.

## Goals

1. An empty but real Android app that installs and launches on a phone.
2. CI that builds, unit-tests and lints every PR and hands over an installable APK.
3. Claude PR review wired in from the first PR.
4. The planning docs (start here, architecture, product design, roadmap, workflow, Play policy) agreed before feature work.
5. The first domain type (`LockState`) in place with tests, so the test pipeline is proven end to end.

## Non-goals

- Any blocking, app picking or NFC behaviour (Phase 1).
- Real artwork, onboarding or store assets (Phase 3).
- Release signing.

## Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| License | GPL-3.0 | Keeps forks open; compatible with F-Droid |
| Package / applicationId | `cc.stadapp.stad` | Matches the owned domain `stadapp.cc`; permanent once on Play |
| Min SDK | 26 (Android 8.0) | Covers ~97% of active devices; simplifies notifications and background limits |
| Target / compile SDK | 36 | Play's target-SDK requirement |
| UI | Jetpack Compose + Material 3 | Current Android standard, less boilerplate |
| Build | Gradle 8.14.3 wrapper, Kotlin DSL, version catalog | One place for versions |
| JDK | 17 (toolchain) | AGP requirement; CI uses Temurin 17 |
| Tests | JUnit 4 on the JVM | Fast, no emulator needed in CI; domain logic lives in pure-Kotlin `core/` |
| Docs location for slice specs and plans | `docs/superpowers/specs/` and `docs/superpowers/plans/` | Same layout as Unnamed-RPG |

## Acceptance

- CI green on PR #1: `assembleDebug`, `testDebugUnitTest` (5 tests), `lintDebug`.
- `stad-debug-apk` artifact installs on David's phone and shows the Stad home screen reading "Unlocked".
- Claude Code Review posts a comment on PR #1.
