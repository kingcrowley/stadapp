# Stad — Play Store Policy

The biggest risk to release is Google Play's review of how Stad uses the **Accessibility API**. This document tracks what Play requires and how Stad meets it. Every slice that touches permissions must keep it accurate. Re-check the linked policy pages before Phase 3, because they change.

## Accessibility API

App blockers may use an AccessibilityService, but it is a restricted use:

| Requirement | How Stad meets it | Slice |
|-------------|-------------------|-------|
| Declare the Accessibility use in the Play Console (permission declaration form) with a video of the feature | Declaration + screen recording of lock → block screen → unlock | 3.2 |
| **Prominent disclosure** in-app before the user enables the service: what is accessed, why, and that it is required | Dedicated onboarding screen before sending the user to system settings; must be shown in normal app flow, not only in a privacy policy | 3.1 |
| Affirmative user consent | "I understand" button on that screen; Stad never enables the service itself | 3.1 |
| Not used to collect personal or sensitive data | Service reads only the foreground package name; nothing is stored beyond the user's own block list; no network permission | 1.2 |
| Service config is minimal | `accessibility_service_config.xml` subscribes only to window-state changes, no `canRetrieveWindowContent` unless a slice proves it needs it | 1.2 |

## Other permissions

| Permission | Play notes | Plan |
|-----------|-----------|------|
| `QUERY_ALL_PACKAGES` | Restricted; needs a declaration and justification | Avoid. Use a `<queries>` element for launcher activities instead (1.1) |
| Device admin (uninstall protection) | Allowed but scrutinised; must be clearly disclosed and removable when unlocked | Decide in the 2.2 spec; disclose in onboarding |
| `RECEIVE_BOOT_COMPLETED`, `NFC` | Normal permissions | — |

## Store listing and accounts

- **Privacy policy URL** required: host at `https://stadapp.cc/privacy` (Scaleway bucket behind Cloudflare). Plain statement: no data collected or shared.
- **Data safety form:** "No data collected", "No data shared".
- **Target API level:** must meet Play's current target-SDK requirement at upload time (currently targeting 36).
- **New personal developer accounts** must run a **closed test with at least 12 testers opted in for 14 continuous days** before applying for production. Recruit testers during Phase 2.
- **Name:** "Stad: NFC App Blocker". Avoid other products' names in the listing or keywords.

## F-Droid

- Must build from source with no proprietary dependencies (no Google Play Services, Firebase, or closed analytics). Keep it that way.
- Anti-features to expect: none planned.
