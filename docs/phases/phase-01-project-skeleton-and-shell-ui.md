# Phase 01: Project skeleton and shell UI

Status: **Done**, checked on the Android 15 emulator; the Infinix check is pending with the client.
Date: 2026-10-27. Build: 0.1.0 (debug), phone and watch.

## Goal and what was built

An installable phone app with the final navigation and working settings, but no listening yet. The Gradle
project has two modules, `:app` (phone) and `:wear` (watch), with the Gradle wrapper and a version catalog.
The phone app has the Home screen from the mockups, bottom navigation to Captions, Alerts and History
(placeholders), and placeholder pages for Sound Categories, Vibration Patterns and Demonstration Mode. Settings
works: text size (5 steps), display mode (standard or high contrast), vibration on/off, and alert threshold
(10-90%). It is saved at once and applied to every screen. An About page states that AGAP is a research
prototype and not a medical device, and lists the licences. Both themes follow the approved palette. The watch
module is a shell with the same package name and signing key. No phone was available, so I checked everything
on an Android 15 emulator.

## Files and modules added or changed

- Build: `settings.gradle`, `build.gradle`, `gradle.properties`, `gradle/libs.versions.toml`,
  `gradle/signing.gradle`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`, `app/build.gradle`,
  `app/lint.xml`, `wear/build.gradle`
- Phone code (`app/src/main/java/ph/agap/app/`): `settings/SettingsStore`, `ui/BaseActivity`,
  `ui/MainActivity`, `ui/PlaceholderFragment`, `ui/home/HomeFragment`, `ui/settings/SettingsActivity`,
  `ui/info/SimplePageActivity`, `SoundCategoriesActivity`, `VibrationPatternsActivity`, `DemoModeActivity`,
  `AboutActivity`
- Phone resources: `colors.xml` (palette), `attrs.xml`, `themes.xml` (light and high contrast), `styles.xml`,
  `strings.xml`, `dimens.xml`, 7 layouts, 2 menus, 15 vector icons, adaptive "AGAP" launcher icon,
  `xml/data_extraction_rules.xml`
- Tests: `app/src/test/.../SettingsStoreTest` (6 tests), `FakeSharedPreferences`
- Watch: `wear/src/main/` manifest, `WatchActivity`, layout, colours, strings, launcher icon
- Docs: PROGRESS, README, DECISIONS (D-007 to D-012), KNOWN-ISSUES, SPEC-CHECKLIST, DEVICE-NOTES, CHANGELOG

## Decisions made and why

D-007 build versions (AGP 9.4.1, Gradle 9.8.2, SDK 36, Groovy scripts, built-in Kotlin off); D-008 package name
`ph.agap.app` for both apps; D-009 one activity with bottom navigation, one tab alive at a time, portrait
lock; D-010 themes through attributes, text size multiplies the phone's font scale; D-011 settings storage,
threshold default 50%, no INTERNET permission, no backup; D-012 plain JUnit tests and the emulator.

## How it was verified

All on the build PC and the `AGAP_API35` emulator (Android 15, 1080x2400).

| Check | Result |
|---|---|
| `gradlew :app:assembleDebug :wear:assembleDebug` | Pass |
| `gradlew :app:testDebugUnitTest` | Pass: 6 tests, 0 failures |
| `gradlew :app:lintDebug` | Pass: 0 errors, 12 accepted warnings (K-003). First run had 1 error and 44 warnings; fixed |
| `gradlew :app:assembleRelease :wear:assembleRelease` | Pass; `apksigner` shows both signed with the release certificate `AB:39:7C:...:80:64` |
| Debug APKs, `apksigner` | Same debug certificate on phone and watch APK; both package `ph.agap.app` |
| Install and open on the emulator | Pass; cold start 1.3 s (`am start -W`) |
| Every screen reachable from Home | Pass: Captions, Alerts, History tabs; Sound Categories, Vibration Patterns, Demonstration Mode, Settings rows; Settings top-bar icon; About from Settings. Back from a tab returns to Home |
| Settings persist after the app is killed | Pass: set high contrast, text step 3, vibration off, threshold 70%; `am force-stop`; relaunched; screen and `shared_prefs/agap_settings.xml` both show the four values |
| High-contrast theme and larger text applied app-wide | Pass (screenshots 09, 10, 12) |
| Permissions (`dumpsys package`) | Only AndroidX's internal signature permission; no INTERNET |
| Crash buffer and error log after the runs | Empty |

**Not verified:** anything on a physical phone or the Infinix Hot 70 (none available); the watch app on a watch
(none); TalkBack reading order (only checked through the layout dump: every control has a label, decorative
icons are hidden); colour contrast was not re-measured, the palette note's values were used as approved.

## Contract items covered

- 1.1 Partly (skeleton app), 1.2 Partly (no internet permission, no backup), 1.7 Partly (all four settings
  saved; text size and contrast applied), 1.8 Partly (Home), 1.9 placeholder, 1.10 structure only,
  1.11 Partly (watch shell, same key), 1.13 Partly (Gradle project with wrapper, README build steps).
- Open: everything that listens (Phases 2-4), the alert screen and Event History (5), the remaining screens (6),
  the watch (7).

## Known issues and scope flags

K-001 to K-005 in KNOWN-ISSUES.md. No scope flags. Phase 0 stays Blocked until a physical phone is on adb.

## Next phase

Phase 2, Live Captions in English: on-device Google speech recognition (Vosk English if offline Google is not
available), the caption screen with the transcript kept in memory only, screen kept on, microphone released
when the tab is left.
