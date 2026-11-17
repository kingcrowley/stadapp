# Stad — Start Here

## What is this?

**Stad** (Irish for "stop") is an open-source Android app that blocks your most distracting apps until you physically tap an NFC tag. Leave the tag somewhere inconvenient (the kitchen, your desk at work) and getting back into Instagram means getting up and walking to it.

- Tap the tag → your chosen apps are **locked**.
- Tap the **same** tag again → they're **unlocked**.
- No in-app unlock button. A rate-limited **sneaky look** (a short peek every few hours) is planned.

## Goals

1. **Strict, not naggy.** It should be genuinely hard to bypass without the tag.
2. **Private.** Offline, no account, no analytics, no network permission.
3. **Open source.** GPL-3.0, published on Google Play and F-Droid.
4. **Built in testable slices.** Every slice is installable and tried on a real phone.

## What you need

- Android Studio (latest stable) with JDK 17
- An Android phone with NFC (Android 8.0 / API 26 or later)
- NFC tags: any **NTAG213/215/216** sticker or card works. Stad uses the tag's unique ID, so nothing has to be written to it.

## Build and run

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # install on a connected phone (USB debugging on)
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew lintDebug            # lint
```

Or open the project in Android Studio and press Run. Every PR's CI run also attaches a ready-to-install debug APK (Actions → the run → Artifacts → `stad-debug-apk`).

## Document map

| Document | Purpose |
|----------|---------|
| `01_START_HERE.md` | This file: what Stad is and how to build it |
| `02_ARCHITECTURE.md` | Layers, packages, data model, Android components |
| `03_PRODUCT_DESIGN.md` | User flows, screens, strict mode, sneaky look, accessibility |
| `04_ROADMAP.md` | Phases and slices with milestones |
| `05_GITHUB_WORKFLOW_GUIDE.md` | Branches, PRs, review, tags |
| `06_PLAY_POLICY.md` | Google Play requirements (Accessibility API, disclosure, privacy) |
| `CHANGELOG.md` | Per-slice detail of what shipped |
| `superpowers/specs/` | One design spec per slice |
| `superpowers/plans/` | One implementation plan per slice |

## Prior art

Studied as reference only (Stad is written from scratch): TapBlok and its fork TapOut (Apache-2.0), nfcGuard (MIT, F-Droid), Normal (iOS, open source), and commercial products Brick, Switchly, Tap Out and The Off Switch.
