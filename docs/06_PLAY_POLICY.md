# Stad — Play Store Policy

The biggest risk to release is Google Play's review of how Stad uses the **Accessibility API**. This document tracks what Play requires and how Stad meets it. Every slice that touches permissions must keep it accurate. Re-check the linked policy pages before Phase 3, because they change.

## Accessibility API

App blockers may use an AccessibilityService, but it is a restricted use:

| Requirement | How Stad meets it | Slice |
|-------------|-------------------|-------|
| Declare the Accessibility use in the Play Console (permission declaration form) with a video of the feature | Declaration + screen recording of lock → block screen → unlock. From 2.1 the declaration also says Stad may return to the home screen once when blocking restarts while locked (after a reboot or update) | 3.2 |
| **Prominent disclosure** in-app before the user enables the service: what is accessed, why, and that it is required | Dedicated onboarding screen before sending the user to system settings; must be shown in normal app flow, not only in a privacy policy | 3.1 |
| Affirmative user consent | "I understand" button on that screen; Stad never enables the service itself | 3.1 |
| Not used to collect personal or sensitive data | The service reads only the foreground app's package name and activity class, uses them to decide whether to block, keeps the last package in memory only, and never stores or logs it. Stad stores only the user's own settings on the device: the block list (1.1), the lock state (1.2), the paired tag's UID (1.3) and sneaky-look timings (2.3). Nothing leaves the device; no network permission | 1.2 |
| Service config is minimal | `res/xml/blocker_accessibility_service.xml` subscribes only to window-state changes, `canRetrieveWindowContent="false"`, not an `isAccessibilityTool` | 1.2 |
| Prominent disclosure before the permission | From 1.2: Home's **Turn on** opens a dialog saying what is accessed (which app and screen are open: the package name and the screen's activity name), why (to cover blocked apps), that it's required for blocking, and that nothing is stored or sent, with an explicit **Agree**; dismissing it is not consent and doesn't open settings. Stad never turns the service on itself. 3.1 polishes this into onboarding | 1.2 / 3.1 |

## Other permissions

| Permission | Play notes | Plan |
|-----------|-----------|------|
| `QUERY_ALL_PACKAGES` | Restricted; needs a declaration and justification | Not used. Slice 1.1 declares `<queries>` for LAUNCHER, HOME, DIAL, SENDTO and SETTINGS intents; slice 1.2 adds the `android.view.InputMethod` service intent, to find enabled keyboards so they are never blocked. No other queries |
| Device admin (uninstall protection) | Allowed but scrutinised; must be clearly disclosed and removable when unlocked | Decide in the 2.2 spec; disclose in onboarding |
| `RECEIVE_BOOT_COMPLETED`, `NFC`, `VIBRATE` | Normal permissions | `NFC` and `VIBRATE` from 1.3 (tag reading/writing and tap feedback). The manifest requires NFC hardware, so Play only offers Stad to phones with NFC. `RECEIVE_BOOT_COMPLETED` from 2.1: check after boot that blocking is still on |
| `POST_NOTIFICATIONS` | Runtime permission (Android 13+), no declaration | From 2.1, for one notification: "Stad is locked, but blocking is off". Asked from a Home card with **Not now**; Stad works without it |

## Store listing and accounts

- **Privacy policy URL** required: host at `https://stadapp.cc/privacy` (Scaleway bucket behind Cloudflare). Plain statement: no data collected or shared.
- **Data safety form:** "No data collected", "No data shared".
- **Target API level:** must meet Play's current target-SDK requirement at upload time (currently targeting 36).
- **New personal developer accounts** must run a **closed test with at least 12 testers opted in for 14 continuous days** before applying for production. Recruit testers during Phase 2.
- **Name:** "Stad: NFC App Blocker". Avoid other products' names in the listing or keywords.

## F-Droid

- Must build from source with no proprietary dependencies (no Google Play Services, Firebase, or closed analytics). Keep it that way.
- Anti-features to expect: none planned.
