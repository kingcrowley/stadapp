# Stad

**Stad: open-source Android app blocker. Tap an NFC tag to lock your chosen apps, tap again to unlock.**

*Stad* is Irish for "stop". Stick an NFC tag somewhere inconvenient, pick the apps that eat your time, and tap the tag to lock them. The only way back in is walking over and tapping it again.

- 🔒 **Strict:** no unlock button in the app. Only your tag unlocks.
- 👀 **Sneaky look** (planned): a short, rate-limited peek every few hours.
- 🛡️ **Private:** offline, no account, no analytics, no internet permission.
- 🧩 **Open source:** GPL-3.0. Coming to Google Play and F-Droid.

> **Status:** early development (Phase 0: Foundation). Not usable yet. See the [roadmap](docs/04_ROADMAP.md).

## Building

Requires Android Studio (latest stable) and JDK 17.

```bash
./gradlew assembleDebug testDebugUnitTest
```

Start with [docs/01_START_HERE.md](docs/01_START_HERE.md).

## Website

[stadapp.cc](https://stadapp.cc)

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).
