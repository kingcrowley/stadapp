# Stad — GitHub Workflow Guide

Solo developer + AI assistant, slice-based delivery, PR-driven integration. Adapted from the Unnamed-RPG workflow.

## Branches

- **`main`** — the integration branch. Always green. No direct commits after the initial commit.
- **`phase/N-sliceX-short-name`** — one branch per slice, e.g. `phase/0-foundation`, `phase/1-slice1-block-list`.
- **`bugfix/short-name`** — fixes between slices; same PR flow.

## Slice workflow

### Start

```bash
git checkout main && git pull
git checkout -b phase/N-sliceX-short-name
```

Write the slice's design spec in `docs/superpowers/specs/YYYY-MM-DD-<slice>-design.md` and its implementation plan (checkbox tasks) in `docs/superpowers/plans/YYYY-MM-DD-<slice>.md`.

### During

Small commits with conventional prefixes and a scope:

```bash
git commit -m "feat(blocker): show block screen for locked packages"
git commit -m "test(core): sneaky look cooldown edges"
git commit -m "fix(nfc): ignore unpaired tags"
```

Run before each commit:

```bash
./gradlew testDebugUnitTest lintDebug
```

### Before opening the PR

- [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug` passes
- [ ] Plan tasks checked off, or deferred with a `TODO(slice-id)` marker
- [ ] `CLAUDE.md` Current Status row and `docs/CHANGELOG.md` updated (with unit-test count)
- [ ] `docs/06_PLAY_POLICY.md` still true if the slice touches permissions
- [ ] Manual test on a real phone of the slice's milestone

### Review

Opening or updating a PR runs:

- **Android CI** (`android-ci.yml`) — build, unit tests, lint; uploads `stad-debug-apk`.
- **Claude Code Review** (`claude-code-review.yml`) — posts a summary review comment. Needs the `CLAUDE_CODE_OAUTH_TOKEN` repo secret.
- **`@claude`** in any PR or issue comment (`claude.yml`) asks Claude to act on it.

### Merge and tag

Squash-merge into `main` (PR title = commit subject). Then:

```bash
git checkout main && git pull
git tag -a Phase1-Slice1-Complete -m "Phase 1 Slice 1: block list"
git push origin Phase1-Slice1-Complete
git branch -d phase/1-slice1-block-list
```

Phase-level tags (`Phase0-Foundation-Complete`) mark the end of a phase.

## Releases (from Phase 3)

- Version: `versionName` follows `0.<phase>.<slice>` until 1.0; `versionCode` increments on every Play upload.
- Release signing keys and keystore passwords are **never** committed (`*.jks`, `keystore.properties` are git-ignored). Play App Signing holds the app signing key.
