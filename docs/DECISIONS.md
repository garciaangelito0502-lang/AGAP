# Decisions

Numbered, newest last. Each entry: what was decided, why, and the phase.

## D-001 Build PC toolchain (Phase 0, 2026-10-27)

- **JDK:** Eclipse Temurin 17.0.20.1+1 (zip, SHA-256 checked against the Adoptium release), unpacked to
  `D:\Android\jdk-17`, set as `JAVA_HOME`. Java 17 because contract 1.1 fixes JDK 17.
- **Android SDK:** installed with Google's `android` CLI 1.1.16717390 into the default
  `%LOCALAPPDATA%\Android\Sdk`: platform-tools 37.0.1 (adb 1.0.41), platforms/android-36 (r02),
  build-tools 36.0.0. No Android Studio on the build PC; the command-line SDK and the Gradle wrapper are enough.
  The project keeps the standard Gradle layout so the client can open it in Android Studio.
- The `android` CLI is run with `--no-metrics` (it sends usage data to Google by default). This is about the
  build tool only; the app itself has no analytics.

## D-002 SDK levels and plugin version: chosen in Phase 1 (Phase 0, 2026-10-27)

Facts gathered for the choice (Android docs, "About Android Gradle plugin", fetched 2026-10-27): the newest AGP
is 9.4 (needs Gradle 9.6.0); API 37 needs AGP 9.1.1 or newer; API 36.1 needs AGP 8.13.0 or newer. Working
assumption: compileSdk and targetSdk 36 (Android 16), minSdk 29 (contract 1.1). The client's test phone runs
Android 15 (API 35). Final versions are recorded here in Phase 1 after a test build.

## D-003 One release keystore for phone and watch (Phase 0, 2026-10-27)

- File: `D:\Project\AGAP\keys\agap-release.jks` on the build PC, **outside this repo**. PKCS12, alias `agap`,
  RSA 4096, SHA384withRSA, valid until 2054-03-14. Certificate SHA-256:
  `AB:39:7C:85:59:D5:1D:52:32:1E:39:61:9C:1C:A5:77:58:17:F8:5F:CE:94:B2:C2:33:20:77:55:B9:2D:80:64`.
- Passwords are in `D:\Project\AGAP\keys\keystore.properties` next to it. Neither file is ever committed
  (`.gitignore` blocks `*.jks`, `*.keystore`, `*.p12`, `keystore.properties`).
- Backed up off the build PC by the developer on 2026-10-27.
- Why one key: the phone and watch apps must share the applicationId **and** the signing key, or the Wear Data
  Layer will not connect them. Debug builds of both modules use the same debug keystore on the build PC, so
  this also holds during development.
- Open (Phase 9): how the keystore is handed to the client. Anyone who ships an update later needs this same
  key, or Android refuses to install the update over the old app.

## D-004 Screen design source (Phase 0, 2026-10-27)

No PNG exports of the client's Figma frames and no AGAP logo were available. The screens follow the screen
mockups made on 2026-10-27 (15 frames: Home, Live Captions, Sound Alerts, Settings, the three alert screens,
Event History, alert detail, Sound Categories, Vibration Patterns, Demonstration Mode, two high-contrast frames
and the watch), which were drawn from the contract text, the readable Figma preview and the approved colour
palette (design note "AGAP colour palette - choice and rationale", 2026-10-27). The palette replaces the Figma
draft's colours. Methods Figure 1 could not be rendered on the build PC. The launcher icon is a plain "AGAP"
text icon. If the client sends Figma exports or a logo later, this entry is updated.

## D-005 Development phone and watch (Phase 0, 2026-10-27)

- No Android phone was available to connect on 2026-10-27. Until one is connected, nothing can be marked
  verified on a device. Items for the client's Infinix Hot 70 stay "pending: client checks it" in
  SPEC-CHECKLIST.md, and each milestone gets a short manual test checklist for the client's team, who reply
  within 2 days of each test build (contract 6.2).
- No Wear OS watch and no watch emulator. The watch app is still built (contract 1.11), but its on-device
  verification is recorded as "not possible" unless a watch becomes available.

## D-006 Repository layout (Phase 0, 2026-10-27)

`agap-app` is its own git repo with modules `:app` (phone) and `:wear` (watch), standard Gradle layout and the
Gradle wrapper, so it opens in Android Studio. Only app source, README and build/install docs go in it; the
contract, research documents and consent letters stay outside. Gradle memory is capped
(`org.gradle.jvmargs=-Xmx2g`, `org.gradle.workers.max=2`, added in Phase 1) because the build PC has a very
small page file.

## D-007 Build versions (Phase 1, 2026-10-27)

Checked against Google's Maven repository and the Android docs on 2026-10-27; newest stable at the time.

| Item | Version | Note |
|---|---|---|
| Android Gradle plugin | 9.4.1 | Needs Gradle 9.6.0+. Opening the project needs **Android Studio Quail 4 (2026.1.4) or newer**. |
| Gradle (wrapper) | 9.8.2 | `distributionSha256Sum` pinned in `gradle-wrapper.properties`. |
| compileSdk / targetSdk | 36 / 36 | API 37 exists; 36 kept (D-002). The Infinix runs Android 15 (API 35). |
| minSdk | 29 phone, 30 watch | Contract 1.1 (Android 10+); Wear OS 3 is API 30. |
| Java | 17 source and target | Contract 1.1. |
| androidx.appcompat | 1.8.0 | |
| com.google.android.material | 1.14.0 | Material 3 for Views. |
| junit | 4.13.2 | JVM unit tests. |

- Build scripts are Groovy (`build.gradle`) with a version catalog (`gradle/libs.versions.toml`), so the project
  holds no Kotlin at all.
- `android.builtInKotlin=false`: AGP 9 turns built-in Kotlin on by default; AGAP has no Kotlin sources. AGP
  prints a warning that this flag goes away in AGP 10 (KNOWN-ISSUES K-001).
- `local.properties` (SDK path) is machine-specific and not committed, as in any Android Studio project.

## D-008 Package name (Phase 1, 2026-10-27)

applicationId `ph.agap.app` for **both** the phone and the watch app (code namespaces `ph.agap.app` and
`ph.agap.wear`). The app is not published on the Play Store, so the name only has to be unique on the test
devices and identical in both apps. It must not change after the first install at the client, or the old
app and its Event History stay behind as a separate app.

## D-009 Screen structure (Phase 1, 2026-10-27)

- One `MainActivity` with bottom navigation (Home, Captions, Alerts, History), as in the mockups. Only the
  selected tab's screen exists (fragments are replaced, not hidden). Live Captions and Sound Alerts will be
  tabs, so switching tabs stops one before the other starts (contract 1.10).
- Settings, Sound Categories, Vibration Patterns, Demonstration Mode and About are separate activities with
  a Back arrow. Home also lists Settings as a labelled row, not only as the top-bar icon.
- Portrait is locked: the test phone stands on a stand, and every trial should look the same. Android 16+
  ignores the lock only on large screens. Lint's orientation warnings are switched off in `app/lint.xml`.

## D-010 Themes and text size (Phase 1, 2026-10-27)

- Two themes: light (`Theme.AGAP`) and high contrast (`Theme.AGAP.HighContrast`). Layouts use theme
  attributes (`?attr/agapText`, `?attr/agapSurface`...) that each theme maps to the palette names in
  `colors.xml`, so every later screen gets both looks without extra work. Category row attributes are already
  defined for Phases 3 and 6.
- High contrast has no thin purple: icons and selected text are white; purple only fills the Home icon
  circles, the selected tab and buttons, always behind white. Slider tracks are grey there.
- The bottom bar has no elevation, because Material 3 would tint it lavender.
- Text size: five steps (0.85, 1.0, 1.2, 1.45, 1.75) that **multiply** the phone's own font size, applied
  to the whole app through the font scale. Default step "Normal" (1.0). When the text size or display mode
  changes, every open screen rebuilds itself when it comes back into view.
- Edge-to-edge on every Android version (Android 15 forces it anyway); the top bar and screen edges are padded
  for the system bars in `BaseActivity`.

## D-011 Settings storage and defaults (Phase 1, 2026-10-27)

- SharedPreferences file `agap_settings`: `text_size_level` (0-4, default 1), `high_contrast` (default off),
  `vibration_on` (default on), `alert_threshold_percent` (10-90 in steps of 5, default **50**). Values are
  clamped when read. The mockup showed 60%; 50% is the starting point until Phase 3 measures real YAMNet scores
  for the test sounds, and it can be tuned under contract 1.12.
- No INTERNET permission. `allowBackup="false"` plus data-extraction rules that exclude everything from cloud
  backup and device transfer, so settings and the Event History stay on the phone (contract 1.2).

## D-012 Tests and test devices (Phase 1, 2026-10-27)

- Unit tests are plain JUnit 4 on the JVM. `SettingsStore` takes a `SharedPreferences`, and the test uses an
  in-memory version, so no Robolectric is needed yet (smaller download, less memory).
- With no phone available (D-005), screens are checked on an Android 15 emulator: virtual device `AGAP_API35`,
  Google Play x86_64 image (revision 9), 2 GB RAM, 1080x2400 at 420 dpi, accelerated by the Windows Hypervisor
  Platform. The emulator says nothing about the Infinix's speed or microphone; those checks stay with the client.
