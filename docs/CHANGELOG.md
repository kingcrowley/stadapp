# Changelog

Per-slice detail. The index is the Current Status table in `CLAUDE.md`.

## Slice 1.1 — Block list (PR #2)

- App picker: searchable (case- and accent-insensitive) list of launchable apps with icons and tick boxes; ticks save immediately. Home shows "N apps blocked" and **Choose apps**.
- `core/`: `AppEntry`, `BlockList`, `PickerRow`, `BlockListRules` (`setBlocked`, `sanitize`, `shouldPersistSanitized`, `effective`, `blockedCount`, `pickerRows`, `ALWAYS_PROTECTED`). The list is read-only while locked (switches on in 1.2).
- `data/`: `BlockListRepository` with an atomic `update`, backed by one app-wide Preferences DataStore; corrupt file reads as empty.
- `service/`: `InstalledApps`, `ProtectedPackages` (Stad, system UI, every home app, dialer, SMS app, Settings), `AppIcons` (cached 48dp bitmaps).
- Protection is applied on read, never written to storage; uninstalled apps are cleaned up only while unlocked and never from an empty app query.
- Manifest `<queries>` for LAUNCHER, HOME, DIAL, SENDTO, SETTINGS (no `QUERY_ALL_PACKAGES`); `dataExtractionRules` keeps settings off cloud backup and device transfer.
- **Unit tests:** 32 (+27: `BlockListRulesTest` 19, `BlockListTest` 3, `DataStoreBlockListRepositoryTest` 5).

## Phase 0 — Foundation (PR #1, merged 2026-11-17)

- Repository set up: GPL-3.0 license, README, `.gitignore`, PR template.
- Docs: `01_START_HERE`, `02_ARCHITECTURE`, `03_PRODUCT_DESIGN`, `04_ROADMAP`, `05_GITHUB_WORKFLOW_GUIDE`, `06_PLAY_POLICY`, this changelog, and `CLAUDE.md`.
- App skeleton: Kotlin + Jetpack Compose + Material 3, package `cc.stadapp.stad`, min SDK 26, target/compile SDK 36, Gradle 8.14.3 wrapper, version catalog.
- `core/LockState` (`Unlocked`/`Locked`, `isLocked`, `toggled()`) — the domain type slices 1.2 and 1.3 build on.
- Placeholder home screen showing name, tagline and lock state; placeholder stop-sign launcher icon.
- CI: `android-ci.yml` (assembleDebug, testDebugUnitTest, lintDebug; uploads reports and the debug APK). Claude review (`claude-code-review.yml`) and `@claude` (`claude.yml`) workflows copied from Unnamed-RPG. CodeRabbit configured as the main PR reviewer (`.coderabbit.yaml`).
- **Unit tests:** 5 (`LockStateTest`).
